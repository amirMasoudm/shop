package org.example.shop1.model.service;

import org.example.shop1.model.entity.ActivityLog;
import org.example.shop1.model.reposritory.ActivityLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.shop1.model.reposritory.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * نوشتن و خواندنِ لاگِ فعالیت. فقط افزودن — هیچ متدِ حذف/ویرایشی عمداً وجود ندارد.
 */
@Service
public class ActivityLogService {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogService.class);

    private final ActivityLogRepository repo;
    private final UserRepository userRepository;
    private final MongoOperations mongoOperations;

    public ActivityLogService(ActivityLogRepository repo, UserRepository userRepository,
                              MongoOperations mongoOperations) {
        this.repo = repo;
        this.userRepository = userRepository;
        this.mongoOperations = mongoOperations;
    }

    /**
     * نامِ خانوادگیِ کاربر برایِ ستونِ «کاربر» در تاریخچه.
     * <p>
     * اگر نامِ خانوادگی نداشت، نامِ کوچک؛ و اگر هیچ‌کدام، {@code null} تا مصرف‌کننده
     * به نامِ کاربری برگردد.
     * <p>
     * ⚠️ یک کوئریِ ایندکس‌دار به‌ازای هر نوشتنِ لاگ، و عمداً بدونِ کش: کشِ بی‌انقضا
     * یعنی اگر مدیر نامِ کارمندی را عوض کند، لاگ‌هایِ <b>تازه</b> هم تا ری‌استارت
     * نامِ قدیمی را ثبت کنند — یعنی داده‌ی غلط در دفتری که قرار است حذف‌نشدنی باشد.
     */
    private String displayNameOf(String username) {
        if (username == null || username.isBlank() || "system".equals(username)) return null;
        try {
            return userRepository.findByUsername(username)
                    .map(u -> {
                        String last = u.getLastName();
                        if (last != null && !last.isBlank()) return last.trim();
                        String first = u.getFirstName();
                        return (first != null && !first.isBlank()) ? first.trim() : null;
                    })
                    .orElse(null);
        } catch (Exception e) {
            return null;   // نامِ نمایشی تزئینی است؛ نبودش نباید جلوی ثبتِ لاگ را بگیرد
        }
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
    /** گفت‌وگوی پشتیبانی — برداشتن، ارجاع، انصراف، بستن. */
    public static final String ENTITY_CONVERSATION = "CONVERSATION";

    public void record(ActivityLog.Action action, ActivityLog.Source source,
                       String entityType, String entityId, String entityName,
                       String field, Object oldValue, Object newValue) {
        try {
            String username = currentUsername();
            ActivityLog entry = new ActivityLog(username, action, source,
                    entityType, entityId, entityName, field, str(oldValue), str(newValue));
            entry.setDisplayName(displayNameOf(username));
            repo.save(entry);
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
            entry.setDisplayName(displayNameOf(username));
            repo.save(entry);
        } catch (Exception e) {
            log.error("ثبتِ لاگِ ورود ناموفق بود: {}", e.toString());
        }
    }

    public Page<ActivityLog> search(String username, Instant from, Instant to, int page, int size) {
        return search(username, null, null, from, to, page, size);
    }

    public Page<ActivityLog> search(String username, String entityType,
                                    Instant from, Instant to, int page, int size) {
        return search(username, entityType, null, from, to, page, size);
    }

    /**
     * جست‌وجویِ تاریخچه با فیلترهایِ اختیاری.
     *
     * @param entityType اگر داده شود فقط همان نوع (مثلاً {@code PRODUCT} یا
     *                   {@code CONVERSATION}).
     * @param action     اگر داده شود فقط همان رویداد (مثلاً {@code CHAT_TRANSFER}).
     *                   رشتهٔ ناشناخته یعنی «بی‌فیلتر»، نه خطا — همان رفتارِ خالی‌بودن.
     */
    public Page<ActivityLog> search(String username, String entityType, String action,
                                    Instant from, Instant to, int page, int size) {
        Instant f = from != null ? from : Instant.EPOCH;
        Instant t = to != null ? to : Instant.now().plusSeconds(86400);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200));

        List<Criteria> parts = new ArrayList<>();
        parts.add(Criteria.where("at").gte(f).lte(t));
        if (username != null && !username.isBlank()) {
            parts.add(Criteria.where("username").is(username.trim()));
        }
        if (entityType != null && !entityType.isBlank()) {
            parts.add(Criteria.where("entityType").is(entityType.trim()));
        }
        ActivityLog.Action parsedAction = parseAction(action);
        if (parsedAction != null) {
            parts.add(Criteria.where("action").is(parsedAction));
        }

        Query query = new Query(new Criteria().andOperator(parts.toArray(new Criteria[0])))
                .with(Sort.by(Sort.Direction.DESC, "at"));
        long total = mongoOperations.count(query, ActivityLog.class);
        List<ActivityLog> rows = mongoOperations.find(query.with(pageable), ActivityLog.class);
        return new PageImpl<>(rows, pageable, total);
    }

    /**
     * ⚠️ ورودیِ کاربر هرگز مستقیم وارد کوئری نمی‌شود: فقط اگر دقیقاً نامِ یکی از
     * مقادیرِ enum باشد فیلتر می‌شود، وگرنه نادیده گرفته می‌شود.
     */
    private ActivityLog.Action parseAction(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return ActivityLog.Action.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
