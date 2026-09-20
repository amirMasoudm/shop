package org.example.shop1.model.service.marketplace;

import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.ActivityLog;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.reposritory.ProductRepository;
import org.example.shop1.model.service.ActivityLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * هماهنگ‌کنندهٔ کفِ قیمتِ رقبا.
 * <p>
 * ⚠️ <b>قاعدهٔ سختِ کسب‌وکار:</b> قیمتِ ما هرگز خودکار از رویِ کفِ بازار تنظیم نمی‌شود.
 * این سرویس فقط {@code …FloorPrice} را می‌نویسد و هشدار می‌دهد؛ تصمیم با انسان است.
 * رقابتِ خودکارِ قیمت به‌سرعت به فروشِ زیانده می‌رسد.
 */
@Service
public class FloorPriceService {

    private static final Logger log = LoggerFactory.getLogger(FloorPriceService.class);

    /** حداقلِ فاصله بینِ دو درخواست به یک بازار (محدودیتِ نرخ). */
    private static final long MIN_REQUEST_GAP_MS = 1500;

    /** کشِ روزانه: قیمتِ تازه‌تر از این بازه دوباره گرفته نمی‌شود. */
    private static final Duration CACHE_TTL = Duration.ofHours(20);

    /** گاردِ عقلانیت: بیرونِ این بازه نسبت به قیمتِ خودمان ذخیره نمی‌شود. */
    private static final BigDecimal SANITY_FACTOR = BigDecimal.TEN;

    /**
     * نامِ خودِ ما در دیجی‌کالا — عیناً همان رشته‌ای که APIِ دیجی‌کالا
     * در {@code default_variant.seller.title} برمی‌گرداند (با پاسخِ واقعی آزموده شد).
     * اگر روزی نامِ غرفه عوض شد، فقط همین خط عوض می‌شود.
     */
    public static final String OUR_DIGIKALA_SELLER = "ارتباطات شبکه داده نما";

    private final ProductRepository productRepo;
    private final ActivityLogService activityLog;
    private final Map<String, MarketplacePriceProvider> providers = new HashMap<>();

    /** آخرین زمانِ درخواست به هر بازار — برایِ فاصله‌گذاری. */
    private final Map<String, Long> lastCallAt = new ConcurrentHashMap<>();

    public FloorPriceService(ProductRepository productRepo, ActivityLogService activityLog,
                             List<MarketplacePriceProvider> providerList) {
        this.productRepo = productRepo;
        this.activityLog = activityLog;
        for (MarketplacePriceProvider p : providerList) {
            providers.put(p.marketKey(), p);
        }
    }

    private MarketplacePriceProvider provider(String market) {
        MarketplacePriceProvider p = providers.get(market);
        if (p == null) throw new ApiException(HttpStatus.BAD_REQUEST, "بازارِ ناشناخته: " + market);
        return p;
    }

    /** آدرسِ جست‌وجویِ آماده — برایِ دکمهٔ «بازکردن در ترب». */
    public String searchPageUrl(String market, String query) {
        return provider(market).searchPageUrl(query);
    }

    /**
     * مرحلهٔ A — نامزدها برایِ تأییدِ انسان.
     * هرگز خودکار انتخاب نمی‌شوند؛ حتی اگر فقط یک نتیجه باشد.
     */
    public List<MarketplacePriceProvider.Candidate> searchCandidates(String market, String query, int limit) {
        MarketplacePriceProvider p = provider(market);
        if (!p.supportsAutomaticFetch()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "این بازار جست‌وجویِ خودکار ندارد؛ از دکمهٔ بازکردنِ صفحه استفاده کن");
        }
        throttle(market);
        return p.searchCandidates(query, Math.min(Math.max(limit, 1), 10));
    }

    /** ذخیرهٔ هویتِ تأییدشده (DKP) — بعد از این، به‌روزرسانی یک‌کلیکی است. */
    public Product linkProduct(String productId, String market, String externalId, String url) {
        Product p = requireProduct(productId);
        if (!"digikala".equals(market)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "اتصالِ شناسه فقط برایِ دیجی‌کالا معنا دارد");
        }
        String oldDkp = p.getDigikalaDkp();
        p.setDigikalaDkp(externalId);
        if (url != null && !url.isBlank()) p.setDigikalaUrl(url);
        p.setUpdatedAt(Instant.now());
        productRepo.save(p);

        activityLog.recordProduct(ActivityLog.Action.FLOOR_PRICE_CHANGE, ActivityLog.Source.MANUAL,
                p.getId(), p.getName(), "digikalaDkp", oldDkp, externalId);
        return p;
    }

    /**
     * مرحلهٔ B — به‌روزرسانیِ قیمت برایِ محصولِ دارایِ شناسه.
     *
     * @param force کشِ روزانه را نادیده بگیر
     * @return نتیجه: {@code status} یکی از ok/skipped/no-id/not-found/out-of-range/unchanged
     */
    public Map<String, Object> refresh(String productId, String market, boolean force) {
        Product p = requireProduct(productId);
        MarketplacePriceProvider prov = provider(market);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("productId", productId);
        res.put("name", nz(p.getName()));

        if (!prov.supportsAutomaticFetch()) {
            res.put("status", "manual-only");
            res.put("message", "این بازار خودکار نیست؛ قیمت را دستی وارد کن");
            return res;
        }

        String externalId = p.getDigikalaDkp();
        if (externalId == null || externalId.isBlank()) {
            res.put("status", "no-id");
            res.put("message", "اول باید محصولِ متناظر انتخاب و تأیید شود");
            return res;
        }

        // کشِ روزانه — درخواستِ بی‌مورد نزن
        if (!force && p.getFloorPriceCheckedAt() != null
                && Duration.between(p.getFloorPriceCheckedAt(), Instant.now()).compareTo(CACHE_TTL) < 0) {
            res.put("status", "skipped");
            res.put("message", "کمتر از ۲۰ ساعت پیش بررسی شده");
            res.put("price", p.getDigikalaFloorPrice());
            return res;
        }

        throttle(market);
        BigDecimal fetched;
        String sellerTitle = null;
        try {
            // یک درخواست، دو خروجی: قیمت و فروشندهٔ باکسِ خرید.
            MarketplacePriceProvider.Snapshot snap = prov.fetchSnapshot(externalId);
            fetched = snap.priceToman();
            sellerTitle = snap.sellerTitle();
        } catch (Exception e) {
            log.warn("خطا در واکشیِ {} برایِ {}: {}", market, productId, e.toString());
            fetched = null;
        }

        // ⚠️ خطا/نبودِ قیمت هرگز مقدارِ قبلی را پاک نمی‌کند
        if (fetched == null) {
            res.put("status", "not-found");
            res.put("message", "قیمتی برگردانده نشد (ممکن است ناموجود باشد) — مقدارِ قبلی حفظ شد");
            res.put("price", p.getDigikalaFloorPrice());
            return res;
        }

        // گاردِ عقلانیت: هم خطایِ واحد را می‌گیرد هم تطبیقِ محصولِ اشتباه را
        BigDecimal ours = p.getOnlinePrice();
        if (ours != null && ours.compareTo(BigDecimal.ZERO) > 0) {
            boolean tooHigh = fetched.compareTo(ours.multiply(SANITY_FACTOR)) > 0;
            boolean tooLow = fetched.multiply(SANITY_FACTOR).compareTo(ours) < 0;
            if (tooHigh || tooLow) {
                res.put("status", "out-of-range");
                res.put("fetched", fetched);
                res.put("ourPrice", ours);
                res.put("message", "عددِ گرفته‌شده بیش از ۱۰ برابر/کمتر از یک‌دهمِ قیمتِ ماست — ذخیره نشد. "
                        + "احتمالاً محصولِ اشتباه وصل شده یا واحد عوض شده.");
                log.warn("گاردِ عقلانیت مانعِ ذخیره شد: {} — گرفته={} مالِ ما={}", p.getName(), fetched, ours);
                return res;
            }
        }

        BigDecimal old = p.getDigikalaFloorPrice();
        p.setDigikalaFloorPrice(fetched);
        // ⚠️ فقط وقتی چیزی آمد بنویس؛ پاسخِ بی‌فروشنده نباید دانستهٔ قبلی را پاک کند.
        if (sellerTitle != null) p.setDigikalaSellerTitle(sellerTitle);
        p.setFloorPriceCheckedAt(Instant.now());
        p.setFloorPriceCheckedBy(activityLog.currentUsername());
        p.setUpdatedAt(Instant.now());
        productRepo.save(p);

        if (old == null || old.compareTo(fetched) != 0) {
            activityLog.recordProduct(ActivityLog.Action.FLOOR_PRICE_CHANGE, ActivityLog.Source.DERIVED,
                    p.getId(), p.getName(), "digikalaFloorPrice", old, fetched);
        }

        res.put("status", old != null && old.compareTo(fetched) == 0 ? "unchanged" : "ok");
        res.put("price", fetched);
        res.put("ourPrice", ours);
        // هشدارِ صرفاً نمایشی؛ قیمتِ ما خودکار عوض نمی‌شود
        res.put("weAreAboveMarket", ours != null && ours.compareTo(fetched) > 0);
        res.put("sellerTitle", p.getDigikalaSellerTitle());
        res.put("weOwnBuyBox", weOwnBuyBox(p));
        return res;
    }

    /**
     * آیا باکسِ خریدِ دیجی‌کالا دستِ خودِ ماست؟
     * <p>
     * وقتی جواب مثبت است، «کفِ بازار» رقیب نیست — خودِ ماییم؛ و کارشناس
     * نباید برایِ جلوزدن از خودمان قیمت را پایین بیاورد.
     */
    public static boolean weOwnBuyBox(Product p) {
        String s = p == null ? null : p.getDigikalaSellerTitle();
        return s != null && normalizeSeller(s).equals(normalizeSeller(OUR_DIGIKALA_SELLER));
    }

    /** نیم‌فاصله/فاصله‌هایِ تکراری را یکدست می‌کند تا مقایسهٔ نام سرِ یک کاراکتر نشکند. */
    private static String normalizeSeller(String s) {
        return s.replace('‌', ' ').replaceAll("\s+", " ").trim();
    }

    /** «به‌روزرسانیِ همه» — فقط محصولاتی که هویتشان قبلاً تأیید شده. */
    public Map<String, Object> refreshAllLinked(String market, boolean force) {
        List<Product> linked = new ArrayList<>();
        for (Product p : productRepo.findAll()) {
            if (p.getDigikalaDkp() != null && !p.getDigikalaDkp().isBlank()) linked.add(p);
        }

        List<Map<String, Object>> results = new ArrayList<>();
        int ok = 0, skipped = 0, failed = 0;
        for (Product p : linked) {
            Map<String, Object> r = refresh(p.getId(), market, force);
            String status = String.valueOf(r.get("status"));
            switch (status) {
                case "ok", "unchanged" -> ok++;
                case "skipped" -> skipped++;
                default -> { failed++; results.add(r); }
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", linked.size());
        summary.put("ok", ok);
        summary.put("skipped", skipped);
        summary.put("failed", failed);
        summary.put("problems", results); // فقط مشکل‌دارها برگردانده می‌شوند
        return summary;
    }

    /** فاصله‌گذاریِ ساده بینِ درخواست‌ها به یک بازار. */
    private void throttle(String market) {
        Long last = lastCallAt.get(market);
        long now = System.currentTimeMillis();
        if (last != null) {
            long wait = MIN_REQUEST_GAP_MS - (now - last);
            if (wait > 0) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        lastCallAt.put(market, System.currentTimeMillis());
    }

    private Product requireProduct(String id) {
        return productRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "محصول یافت نشد"));
    }

    private String nz(String s) { return s == null ? "" : s; }
}
