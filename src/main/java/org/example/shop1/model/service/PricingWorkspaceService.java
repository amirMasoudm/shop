package org.example.shop1.model.service;

import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.dto.PricingRowDto;
import org.example.shop1.model.entity.ActivityLog;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.reposritory.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * منطقِ میزِ کارِ قیمت‌گذاری: خواندنِ ردیف‌ها و ذخیرهٔ دسته‌ایِ تغییرات با لاگ.
 * <p>
 * موجودی‌ها در این فاز عمداً نوشتنی نیستند (مرزِ اسکوپ: موجودی فقط از ورودِ دسته‌ای
 * و بعداً هلو ست می‌شود) — هر تلاشی برایِ نوشتنشان از این مسیر نادیده گرفته می‌شود.
 */
@Service
public class PricingWorkspaceService {

    /** فیلدهایِ قابلِ ویرایش از میزِ کار. هر چیزِ دیگری رد می‌شود. */
    private static final List<String> EDITABLE = List.of(
            "onlinePrice", "partnerUnitPrice", "partnerBulkPrice", "dollarPrice",
            "torobFloorPrice", "torobUrl",
            "digikalaFloorPrice", "digikalaUrl",
            "pushSaleFlag",
            "priceOverride"); // فقط برایِ «بازگشت به فرمول» (false کردن)

    /** ضریبِ «تلهٔ هزاربرابری» — بیش از این نسبت، تأییدِ دوم لازم دارد. */
    private static final BigDecimal SPIKE_FACTOR = BigDecimal.valueOf(10);

    /**
     * فیلدهایی که کارشناسِ فروش ({@code SALES}) اجازهٔ تغییرشان را ندارد.
     * <p>
     * «فروش تعدادی» ردهٔ قیمتِ عمده است و قیمتِ سایت از رویش مشتق می‌شود، پس
     * تغییرش اثرِ زنجیره‌ای روی قیمتِ عمومی دارد — تصمیمش مالِ کارشناسِ قیمت‌گذاری
     * است. بقیهٔ فیلدها برای هر دو نقش باز است.
     */
    private static final List<String> PRICER_ONLY_FIELDS = List.of("partnerBulkPrice");

    private final ProductRepository productRepo;
    private final ActivityLogService activityLog;
    private final StoreSettingsService settingsService;

    public PricingWorkspaceService(ProductRepository productRepo, ActivityLogService activityLog,
                                   StoreSettingsService settingsService) {
        this.productRepo = productRepo;
        this.activityLog = activityLog;
        this.settingsService = settingsService;
    }

    /**
     * قیمتِ سایت مشتق از «فروش تعدادی»: {@code partnerBulkPrice × sitePriceFactor}.
     * ضریب از تنظیمات می‌آید، نه هاردکد — تا دو منبعِ حقیقت نداشته باشیم.
     *
     * @return مقدارِ محاسبه‌شده، یا {@code null} اگر «فروش تعدادی» خالی باشد
     */
    public BigDecimal derivedOnlinePrice(Product p, BigDecimal factor) {
        BigDecimal bulk = p.getPartnerBulkPrice();
        if (bulk == null || bulk.compareTo(BigDecimal.ZERO) <= 0) return null;
        return bulk.multiply(factor).setScale(0, java.math.RoundingMode.HALF_UP);
    }

    /**
     * بعد از تغییرِ «فروش تعدادی»، قیمتِ سایت را بازمحاسبه می‌کند —
     * مگر اینکه قیمت دستی ست شده باشد ({@code priceOverride})، که در آن صورت
     * عمداً دست نمی‌خورد و کارشناس در UI می‌بیند چقدر با فرمول فاصله دارد.
     */
    private void recalcOnlinePriceIfDerived(Product p, BigDecimal factor) {
        BigDecimal current = p.getOnlinePrice();
        boolean empty = current == null || current.compareTo(BigDecimal.ZERO) <= 0;

        // 🔴 پرچمِ دستی فقط از یک «تصمیمِ آگاهانه» محافظت می‌کند — و قیمتِ خالی تصمیم
        // نیست. محصولی که قیمتِ سایتش خالی است اصلاً قیمتِ مشتری‌رو ندارد، پس وقتی
        // کارشناس «فروش تعدادی» را وارد می‌کند باید همان‌جا از فرمول پر شود. پیش از
        // این، اگر کسی قیمت را خالی کرده بود پرچمِ دستی روشن می‌ماند و محصول برای
        // همیشه بی‌قیمت می‌ماند بی‌آنکه جایی هشدار بدهد.
        if (Boolean.TRUE.equals(p.getPriceOverride()) && !empty) return;

        BigDecimal derived = derivedOnlinePrice(p, factor);
        if (derived == null) return;

        // قیمت که از فرمول آمد، دیگر «دستی» نیست
        if (empty) p.setPriceOverride(false);

        BigDecimal before = p.getOnlinePrice();
        if (before != null && before.compareTo(derived) == 0) return; // تغییری نکرده

        p.setOnlinePrice(derived);
        // منبعِ متفاوت تا در لاگ معلوم باشد این تغییر خودکار بوده نه دستی
        activityLog.recordProduct(ActivityLog.Action.PRICE_CHANGE, ActivityLog.Source.DERIVED,
                p.getId(), p.getName(), "onlinePrice", before, derived);
    }

    public List<PricingRowDto> rows(String query) {
        List<Product> all = productRepo.findAll();
        String q = query == null ? "" : query.trim().toLowerCase();
        BigDecimal factor = settingsService.getSitePriceFactor();

        List<PricingRowDto> out = new ArrayList<>();
        for (Product p : all) {
            if (!q.isEmpty()) {
                String name = p.getName() == null ? "" : p.getName().toLowerCase();
                if (!name.contains(q)) continue;
            }
            out.add(PricingRowDto.of(p, derivedOnlinePrice(p, factor)));
        }
        out.sort(Comparator.comparing(PricingRowDto::getName, Comparator.nullsLast(String::compareTo)));
        return out;
    }

    /**
     * ذخیرهٔ دسته‌ای. هر آیتم: {@code {id, field, value, confirmed?}}.
     * <p>
     * اگر تغییرِ قیمت بیش از ۱۰ برابر یا کمتر از یک‌دهمِ مقدارِ قبلی باشد و
     * {@code confirmed=true} نیامده باشد، آن ردیف رد می‌شود و در {@code needsConfirm}
     * برمی‌گردد تا فرانت تأییدِ دوم بگیرد.
     */
    public Map<String, Object> applyBatch(List<Map<String, Object>> changes) {
        if (changes == null || changes.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "هیچ تغییری ارسال نشده است");
        }

        List<Map<String, Object>> needsConfirm = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int applied = 0;
        // یک‌بار خوانده می‌شود، نه به‌ازایِ هر ردیف
        final BigDecimal factor = settingsService.getSitePriceFactor();

        for (Map<String, Object> ch : changes) {
            String id = str(ch.get("id"));
            String field = str(ch.get("field"));
            Object rawValue = ch.get("value");
            boolean confirmed = Boolean.TRUE.equals(ch.get("confirmed"))
                    || "true".equalsIgnoreCase(String.valueOf(ch.get("confirmed")));

            if (id == null || field == null) {
                errors.add("ردیفِ ناقص (id یا field ندارد)");
                continue;
            }
            if (!EDITABLE.contains(field)) {
                errors.add("فیلدِ غیرمجاز برایِ ویرایش: " + field);
                continue;
            }
            // مرزِ فیلدیِ نقش‌ها — سمتِ سرور، نه فقط readonly در UI
            if (PRICER_ONLY_FIELDS.contains(field) && !canEditPricerOnlyFields()) {
                errors.add("«فروش تعدادی» فقط توسطِ کارشناسِ قیمت‌گذاری قابلِ تغییر است");
                continue;
            }

            Product p = productRepo.findById(id).orElse(null);
            if (p == null) {
                errors.add("محصول یافت نشد: " + id);
                continue;
            }

            try {
                switch (field) {
                    case "onlinePrice", "partnerUnitPrice", "partnerBulkPrice", "dollarPrice",
                         "torobFloorPrice", "digikalaFloorPrice" -> {
                        BigDecimal newVal = toDecimal(rawValue);
                        BigDecimal oldVal = switch (field) {
                            case "onlinePrice" -> p.getOnlinePrice();
                            case "partnerUnitPrice" -> p.getPartnerUnitPrice();
                            case "partnerBulkPrice" -> p.getPartnerBulkPrice();
                            case "dollarPrice" -> p.getDollarPrice();
                            case "torobFloorPrice" -> p.getTorobFloorPrice();
                            default -> p.getDigikalaFloorPrice();
                        };

                        if (isSpike(oldVal, newVal) && !confirmed) {
                            needsConfirm.add(Map.of(
                                    "id", id,
                                    "name", nz(p.getName()),
                                    "field", field,
                                    "oldValue", oldVal == null ? "" : oldVal.toPlainString(),
                                    "newValue", newVal == null ? "" : newVal.toPlainString()));
                            continue;
                        }

                        switch (field) {
                            case "onlinePrice" -> {
                                p.setOnlinePrice(newVal);
                                // ویرایشِ مستقیمِ قیمتِ سایت = تصمیمِ آگاهانه‌ی انسان؛
                                // از این به بعد فرمول بازنویسی‌اش نمی‌کند
                                p.setPriceOverride(true);
                            }
                            case "partnerUnitPrice" -> p.setPartnerUnitPrice(newVal);
                            case "partnerBulkPrice" -> {
                                p.setPartnerBulkPrice(newVal);
                                recalcOnlinePriceIfDerived(p, factor);
                            }
                            case "dollarPrice" -> p.setDollarPrice(newVal);
                            case "torobFloorPrice" -> {
                                p.setTorobFloorPrice(newVal);
                                stampFloorCheck(p);
                            }
                            default -> {
                                p.setDigikalaFloorPrice(newVal);
                                stampFloorCheck(p);
                            }
                        }

                        p.setUpdatedAt(Instant.now());
                        productRepo.save(p);
                        activityLog.recordProduct(
                                field.contains("Floor") ? ActivityLog.Action.FLOOR_PRICE_CHANGE
                                        : ActivityLog.Action.PRICE_CHANGE,
                                ActivityLog.Source.MANUAL,
                                p.getId(), p.getName(), field, oldVal, newVal);
                        applied++;
                    }
                    case "torobUrl", "digikalaUrl" -> {
                        String newVal = str(rawValue);
                        String oldVal = "torobUrl".equals(field) ? p.getTorobUrl() : p.getDigikalaUrl();
                        if ("torobUrl".equals(field)) p.setTorobUrl(newVal); else p.setDigikalaUrl(newVal);
                        p.setUpdatedAt(Instant.now());
                        productRepo.save(p);
                        activityLog.recordProduct(ActivityLog.Action.FLOOR_PRICE_CHANGE, ActivityLog.Source.MANUAL,
                                p.getId(), p.getName(), field, oldVal, newVal);
                        applied++;
                    }
                    case "pushSaleFlag" -> {
                        Boolean newVal = Boolean.TRUE.equals(rawValue)
                                || "true".equalsIgnoreCase(String.valueOf(rawValue));
                        Boolean oldVal = p.getPushSaleFlag();
                        p.setPushSaleFlag(newVal);
                        p.setUpdatedAt(Instant.now());
                        productRepo.save(p);
                        activityLog.recordProduct(ActivityLog.Action.FLAG_CHANGE, ActivityLog.Source.MANUAL,
                                p.getId(), p.getName(), field, oldVal, newVal);
                        applied++;
                    }
                    // «بازگشت به فرمول»: پرچمِ دستی برداشته و قیمت دوباره مشتق می‌شود.
                    // فقط برداشتنِ پرچم پذیرفته است؛ روشن‌کردنش با ویرایشِ مستقیمِ قیمت انجام می‌شود.
                    case "priceOverride" -> {
                        boolean requested = Boolean.TRUE.equals(rawValue)
                                || "true".equalsIgnoreCase(String.valueOf(rawValue));
                        if (requested) {
                            errors.add("روشن‌کردنِ «قیمتِ دستی» مستقیم ممکن نیست؛ کافیست قیمتِ سایت را ویرایش کنی");
                            continue;
                        }
                        Boolean oldVal = p.getPriceOverride();
                        p.setPriceOverride(false);
                        recalcOnlinePriceIfDerived(p, factor);
                        p.setUpdatedAt(Instant.now());
                        productRepo.save(p);
                        activityLog.recordProduct(ActivityLog.Action.FLAG_CHANGE, ActivityLog.Source.MANUAL,
                                p.getId(), p.getName(), field, oldVal, false);
                        applied++;
                    }
                    default -> errors.add("فیلدِ ناشناخته: " + field);
                }
            } catch (NumberFormatException e) {
                errors.add("مقدارِ عددیِ نامعتبر برایِ " + field + " در " + nz(p.getName()));
            }
        }

        return Map.of("applied", applied, "needsConfirm", needsConfirm, "errors", errors);
    }

    private void stampFloorCheck(Product p) {
        p.setFloorPriceCheckedAt(Instant.now());
        p.setFloorPriceCheckedBy(activityLog.currentUsername());
    }

    /**
     * ADMIN و کارشناسِ ارشد (PRICER) می‌توانند قیمت‌ها را تغییر دهند؛ کارشناسِ فروشِ
     * و قیمت‌گذاری (SALES) و کارشناسِ فروش (SUPPORT) فقط مشاهده.
     * <p>
     * این متد قبلاً مرزِ «فروش تعدادی» بود و بعد به ADMIN محدود شد. حالا که مالک
     * کارشناسِ ارشد را هم با «همان اختیارِ ادمین در میز» اضافه کرده، همین‌جا هم باز
     * می‌شود — وگرنه SecurityConfig درخواست را رد نمی‌کرد ولی این لایه بی‌صدا
     * ردش می‌کرد و کاربر فقط یک پیامِ گمراه‌کننده می‌دید.
     * <p>
     * UI هم با همین تصمیم می‌گیرد کدام خانه قابلِ تایپ باشد.
     */
    public boolean canEditPricerOnlyFields() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(Object::toString)
                .anyMatch(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_PRICER"));
    }

    /** جهشِ مشکوک: بیش از ۱۰ برابر یا کمتر از یک‌دهم. مقدارِ قبلیِ خالی/صفر جهش حساب نمی‌شود. */
    private boolean isSpike(BigDecimal oldVal, BigDecimal newVal) {
        if (oldVal == null || newVal == null) return false;
        if (oldVal.compareTo(BigDecimal.ZERO) <= 0 || newVal.compareTo(BigDecimal.ZERO) <= 0) return false;
        return newVal.compareTo(oldVal.multiply(SPIKE_FACTOR)) > 0
                || newVal.multiply(SPIKE_FACTOR).compareTo(oldVal) < 0;
    }

    private BigDecimal toDecimal(Object raw) {
        if (raw == null) return null;
        String s = String.valueOf(raw).trim().replace(",", "");
        if (s.isEmpty()) return null;
        return new BigDecimal(s);
    }

    private String str(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private String nz(String s) { return s == null ? "" : s; }
}
