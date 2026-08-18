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
            "pushSaleFlag");

    /** ضریبِ «تلهٔ هزاربرابری» — بیش از این نسبت، تأییدِ دوم لازم دارد. */
    private static final BigDecimal SPIKE_FACTOR = BigDecimal.valueOf(10);

    private final ProductRepository productRepo;
    private final ActivityLogService activityLog;

    public PricingWorkspaceService(ProductRepository productRepo, ActivityLogService activityLog) {
        this.productRepo = productRepo;
        this.activityLog = activityLog;
    }

    public List<PricingRowDto> rows(String query) {
        List<Product> all = productRepo.findAll();
        String q = query == null ? "" : query.trim().toLowerCase();

        List<PricingRowDto> out = new ArrayList<>();
        for (Product p : all) {
            if (!q.isEmpty()) {
                String name = p.getName() == null ? "" : p.getName().toLowerCase();
                if (!name.contains(q)) continue;
            }
            out.add(PricingRowDto.of(p));
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
                            case "onlinePrice" -> p.setOnlinePrice(newVal);
                            case "partnerUnitPrice" -> p.setPartnerUnitPrice(newVal);
                            case "partnerBulkPrice" -> p.setPartnerBulkPrice(newVal);
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
                        activityLog.record(
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
                        activityLog.record(ActivityLog.Action.FLOOR_PRICE_CHANGE, ActivityLog.Source.MANUAL,
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
                        activityLog.record(ActivityLog.Action.FLAG_CHANGE, ActivityLog.Source.MANUAL,
                                p.getId(), p.getName(), field, oldVal, newVal);
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
