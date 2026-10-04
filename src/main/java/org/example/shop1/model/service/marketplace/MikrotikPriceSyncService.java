package org.example.shop1.model.service.marketplace;

import org.example.shop1.model.entity.ActivityLog;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.reposritory.ProductRepository;
import org.example.shop1.model.service.ActivityLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * تطبیقِ محصولاتِ ما با کاتالوگِ میکروتیک و نوشتنِ «مرجع $».
 * <p>
 * <b>🔴 هیچ‌چیز خودکار اعمال نمی‌شود.</b> همان قاعده‌ای که برایِ دیجی‌کالا
 * گذاشتیم: پیشنهاد ساخته می‌شود، انسان می‌بیند و تأیید می‌کند، بعد نوشته
 * می‌شود. قیمتِ اشتباه روی محصولِ اشتباه، بدتر از نبودنِ قیمت است.
 * <p>
 * <b>مبنایِ تطبیق کدِ قطعه است، نه نام:</b> نامِ محصولاتِ ما فارسی است ولی
 * کدِ قطعه («RBcAPGi-5acD2nD») تقریباً همیشه داخلش نوشته شده. کد یکتاست و
 * نامِ بازاریابی نه — «cAP ac» در «cAP ac XL» هم هست.
 */
@Service
public class MikrotikPriceSyncService {

    private static final Logger log = LoggerFactory.getLogger(MikrotikPriceSyncService.class);

    /** کوتاه‌تر از این، تطبیق بی‌معنی می‌شود («ac» در هر نامی هست). */
    private static final int MIN_CODE_LEN = 5;

    private final ProductRepository productRepo;
    private final MikrotikCatalogService catalog;
    private final ActivityLogService activityLog;

    public MikrotikPriceSyncService(ProductRepository productRepo,
                                    MikrotikCatalogService catalog,
                                    ActivityLogService activityLog) {
        this.productRepo = productRepo;
        this.catalog = catalog;
        this.activityLog = activityLog;
    }

    /**
     * پیشنهادها برایِ تأییدِ انسان.
     * <p>
     * هر محصولی که کدِ یکی از قطعاتِ کاتالوگ داخلِ نامش باشد، یک پیشنهاد
     * می‌گیرد. اگر چند کد بخورد، <b>بلندترین</b> برنده است — یعنی دقیق‌ترین.
     */
    public Map<String, Object> proposals() {
        Map<String, MikrotikCatalogService.Entry> cat = catalog.snapshot();
        List<Map<String, Object>> out = new ArrayList<>();
        int unchanged = 0;

        for (Product p : productRepo.findAll()) {
            String nname = MikrotikCatalogService.normalizeCode(p.getName());
            if (nname.isEmpty()) continue;

            MikrotikCatalogService.Entry best = null;
            int bestLen = 0;
            for (Map.Entry<String, MikrotikCatalogService.Entry> e : cat.entrySet()) {
                String code = e.getKey();
                if (code.length() < MIN_CODE_LEN || code.length() <= bestLen) continue;
                if (nname.contains(code)) {
                    best = e.getValue();
                    bestLen = code.length();
                }
            }
            if (best == null) continue;

            BigDecimal cur = p.getDollarPrice();
            if (cur != null && cur.compareTo(best.usd()) == 0) { unchanged++; continue; }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", p.getId());
            row.put("name", p.getName());
            row.put("code", best.code());
            row.put("mikrotikName", best.name());
            row.put("current", cur);
            row.put("usd", best.usd());
            row.put("url", best.url());
            out.add(row);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("catalogSize", cat.size());
        res.put("unchanged", unchanged);
        res.put("proposals", out);
        return res;
    }

    /** نوشتنِ همان‌هایی که کاربر تأیید کرده — نه بیشتر. */
    public Map<String, Object> apply(List<Map<String, Object>> picks) {
        int applied = 0;
        List<String> errors = new ArrayList<>();

        for (Map<String, Object> pick : picks == null ? List.<Map<String, Object>>of() : picks) {
            String id = pick.get("id") == null ? null : String.valueOf(pick.get("id"));
            if (id == null) continue;

            BigDecimal usd;
            try {
                usd = new BigDecimal(String.valueOf(pick.get("usd")));
            } catch (Exception e) {
                errors.add("قیمتِ نامعتبر برایِ " + id);
                continue;
            }
            if (usd.signum() <= 0) { errors.add("قیمتِ صفر برایِ " + id); continue; }

            Product p = productRepo.findById(id).orElse(null);
            if (p == null) { errors.add("محصول یافت نشد: " + id); continue; }

            BigDecimal old = p.getDollarPrice();
            if (old != null && old.compareTo(usd) == 0) continue;

            p.setDollarPrice(usd);
            p.setUpdatedAt(Instant.now());
            productRepo.save(p);
            activityLog.recordProduct(ActivityLog.Action.PRICE_CHANGE, ActivityLog.Source.DERIVED,
                    p.getId(), p.getName(), "dollarPrice", old, usd);
            applied++;
        }

        log.info("مرجعِ دلاریِ میکروتیک: {} محصول به‌روز شد", applied);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("applied", applied);
        res.put("errors", errors);
        return res;
    }
}
