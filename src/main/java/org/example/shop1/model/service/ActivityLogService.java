package org.example.shop1.model.service;

import org.example.shop1.model.entity.ActivityLog;
import org.example.shop1.model.reposritory.ActivityLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * نوشتن و خواندنِ لاگِ فعالیت. فقط افزودن — هیچ متدِ حذف/ویرایشی عمداً وجود ندارد.
 */
@Service
public class ActivityLogService {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogService.class);

    private final ActivityLogRepository repo;

    public ActivityLogService(ActivityLogRepository repo) {
        this.repo = repo;
    }

    /** نامِ کاربرِ فعلی از کانتکستِ امنیتی؛ اگر نبود "system". */
    public String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "system";
        }
        return auth.getName();
    }

    /** نوع‌هایِ شناخته‌شده‌ی موجودیت (رشته‌اند تا افزودنِ نوعِ جدید نیازی به تغییرِ هسته نداشته باشد). */
    public static final String ENTITY_PRODUCT = "PRODUCT";
    public static final String ENTITY_SETTINGS = "SETTINGS";
    /** دادهٔ رفتاریِ مشتریان — خروجی‌گرفتن و حذفش کنشِ کارمندیِ قابلِ ممیزی است. */
    public static final String ENTITY_ANALYTICS = "ANALYTICS";

    public void record(ActivityLog.Action action, ActivityLog.Source source,
                       String entityType, String entityId, String entityName,
                       String field, Object oldValue, Object newValue) {
        try {
            repo.save(new ActivityLog(currentUsername(), action, source,
                    entityType, entityId, entityName, field, str(oldValue), str(newValue)));
        } catch (Exception e) {
            // شکستِ لاگ نباید جلوی خودِ عملیات را بگیرد، ولی بی‌صدا هم نماند
            log.error("ثبتِ لاگِ فعالیت ناموفق بود ({} روی {}/{}): {}", field, entityType, entityId, e.toString());
        }
    }

    /** میان‌بُرِ رویدادهایِ محصول (پرتکرارترین حالت). */
    public void recordProduct(ActivityLog.Action action, ActivityLog.Source source,
                              String productId, String productName,
                              String field, Object oldValue, Object newValue) {
        record(action, source, ENTITY_PRODUCT, productId, productName, field, oldValue, newValue);
    }

    public void recordLogin(String username) {
        try {
            ActivityLog entry = new ActivityLog(username, ActivityLog.Action.LOGIN,
                    ActivityLog.Source.MANUAL, null, null, null, null, null, null);
            repo.save(entry);
        } catch (Exception e) {
            log.error("ثبتِ لاگِ ورود ناموفق بود: {}", e.toString());
        }
    }

    public Page<ActivityLog> search(String username, Instant from, Instant to, int page, int size) {
        return search(username, null, from, to, page, size);
    }

    /**
     * @param entityType اگر داده شود فقط همان نوع (مثلاً {@code PRODUCT}) — پایه‌ی
     *                   دو قسمتی‌شدنِ صفحه‌ی لاگ (قیمت‌گذاری در برابرِ پنلِ ادمین).
     */
    public Page<ActivityLog> search(String username, String entityType,
                                    Instant from, Instant to, int page, int size) {
        Instant f = from != null ? from : Instant.EPOCH;
        Instant t = to != null ? to : Instant.now().plusSeconds(86400);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200));

        boolean hasUser = username != null && !username.isBlank();
        boolean hasType = entityType != null && !entityType.isBlank();

        if (hasUser && hasType) {
            return repo.findByUsernameAndEntityTypeAndAtBetweenOrderByAtDesc(
                    username.trim(), entityType.trim(), f, t, pageable);
        }
        if (hasType) {
            return repo.findByEntityTypeAndAtBetweenOrderByAtDesc(entityType.trim(), f, t, pageable);
        }
        if (hasUser) {
            return repo.findByUsernameAndAtBetweenOrderByAtDesc(username.trim(), f, t, pageable);
        }
        return repo.findByAtBetweenOrderByAtDesc(f, t, pageable);
    }

    private String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
