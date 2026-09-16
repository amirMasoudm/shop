package org.example.shop1.model.service.analytics;

import jakarta.servlet.http.HttpServletRequest;
import org.example.shop1.config.AnalyticsProperties;
import org.example.shop1.model.entity.UserEvent;
import org.example.shop1.model.entity.Visitor;
import org.example.shop1.model.enums.Channel;
import org.example.shop1.model.enums.EventType;
import org.example.shop1.model.reposritory.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * تنها نقطهٔ نوشتنِ رویدادهای رفتاری.
 * <p>
 * 🔴 <b>نوشتن نباید سایت را کُند کند.</b> رویداد داخلِ مسیرِ درخواست در یک صفِ
 * <b>کران‌دارِ</b> حافظه می‌نشیند و یک تسکِ زمان‌بندی‌شده هر دو ثانیه با
 * {@code insertMany} خالی‌اش می‌کند. <b>صف که پر شد، رویداد دور ریخته می‌شود</b> —
 * یک رویدادِ گم‌شده ضرری ندارد، یک صفحهٔ کند برایِ همهٔ کاربران دارد. سرور دو هسته و
 * ۳٫۸ گیگ رم دارد، پس صف کرانِ بالا دارد نه رشدِ آزاد.
 * <p>
 * هیچ رویدادی داخلِ تراکنشِ سفارش نمی‌نشیند: نوشتنِ واقعی همیشه در نخِ زمان‌بندی
 * انجام می‌شود، نه در نخِ درخواست.
 */
@Service
public class UserEventRecorder {

    private static final Logger log = LoggerFactory.getLogger(UserEventRecorder.class);

    /** بیشترین انحرافِ مجازِ زمانِ کلاینت؛ بیرونِ این بازه به زمانِ سرور برمی‌گردد. */
    private static final Duration MAX_CLOCK_SKEW_PAST = Duration.ofMinutes(10);
    private static final Duration MAX_CLOCK_SKEW_FUTURE = Duration.ofMinutes(1);

    private final AnalyticsProperties props;
    private final MongoOperations mongo;
    private final UserRepository userRepository;

    private final BlockingQueue<UserEvent> queue;

    /** نامِ کاربری → شناسه. کاربرانِ کارکنان و مشتریانِ فعال کم‌اند؛ کش عملاً رایگان است. */
    private final Map<String, String> userIdCache = new ConcurrentHashMap<>();

    private final AtomicLong dropped = new AtomicLong();

    /** آخرین زمانِ صادرشده — نگهبانِ یکتاییِ ترتیب؛ نگاه کن به distinct(). */
    private final java.util.concurrent.atomic.AtomicReference<Instant> lastIssuedAt =
            new java.util.concurrent.atomic.AtomicReference<>();

    public UserEventRecorder(AnalyticsProperties props, MongoOperations mongo, UserRepository userRepository) {
        this.props = props;
        this.mongo = mongo;
        this.userRepository = userRepository;
        this.queue = new ArrayBlockingQueue<>(props.getQueueCapacity());
    }

    // ==========================================================
    // ثبت
    // ==========================================================

    /** ثبت با زمینهٔ درخواستِ جاری — مسیرِ معمولِ قلاب‌های داخلِ سرویس‌ها. */
    public void record(EventType type, String entityType, String entityId, String entityName,
                       Map<String, Object> props) {
        HttpServletRequest request = currentRequest();
        if (request == null) return;   // نخِ پس‌زمینه؛ زمینه‌ای برایِ نسبت‌دادن نیست
        AnalyticsContext ctx = (AnalyticsContext) request.getAttribute(AnalyticsContext.REQUEST_ATTRIBUTE);
        if (ctx == null) return;
        record(ctx, type, request.getRequestURI(), entityType, entityId, entityName, props, null);
    }

    public void record(EventType type, String entityType, String entityId, String entityName) {
        record(type, entityType, entityId, entityName, null);
    }

    /** ثبتِ صریح — بیکن و اینترسپتور که خودشان مسیر و زمان را می‌دانند. */
    public void record(AnalyticsContext ctx, EventType type, String path,
                       String entityType, String entityId, String entityName,
                       Map<String, Object> rawProps, Instant clientAt) {
        if (ctx == null || type == null) return;
        if (isStaffTraffic()) return;

        UserEvent event = new UserEvent();
        event.setAt(clampTime(clientAt));
        event.setAnonId(ctx.anonId());
        event.setSessionId(ctx.sessionId());
        event.setUserId(currentUserId());
        event.setType(type);
        event.setPath(trim(path, 300));
        event.setEntityType(trim(entityType, 40));
        event.setEntityId(trim(entityId, 60));
        event.setEntityName(trim(entityName, 200));
        event.setProps(limitProps(rawProps));
        event.setDevice(ctx.device());
        event.setCity(ctx.city());
        // 🔴 کانال و کارزار روی هر رویداد می‌نشینند، نه فقط روی SESSION_START.
        event.setChannel(ctx.channel() == null ? Channel.DIRECT : ctx.channel());
        event.setCampaign(ctx.campaign());

        if (!queue.offer(event)) {
            long total = dropped.incrementAndGet();
            // فقط گاهی لاگ شود، وگرنه خودِ لاگ به مشکلِ بعدی تبدیل می‌شود
            if (total % 1000 == 1) {
                log.warn("صفِ رویدادها پر است؛ تا اینجا {} رویداد دور ریخته شد.", total);
            }
        }
    }

    // ==========================================================
    // بازدیدکننده
    // ==========================================================

    /**
     * آپسرتِ سندِ بازدیدکننده در شروعِ هر بازدید.
     * <p>
     * 🔴 {@code firstTouch} و {@code firstSeenAt} با {@code $setOnInsert} نوشته می‌شوند،
     * یعنی <b>فقط یک‌بار</b>. اگر بازنویسی می‌شدند، کسی که اولین‌بار از ترب آمده و
     * هفتهٔ بعد مستقیم برگشته، اعتبارِ فروشش به «مستقیم» می‌رفت و ترب صفر می‌شد.
     * <p>
     * فقط از {@code SESSION_START} صدا زده می‌شود، نه از هر مشاهدهٔ صفحه.
     */
    public void touchVisitor(AnalyticsContext ctx, TrafficSource source) {
        if (ctx == null || ctx.anonId() == null) return;
        // بدونِ این، کارمند در جدولِ «کاربران» به‌عنوان بازدیدکننده ظاهر می‌شد حتی
        // وقتی هیچ رویدادی برایش ثبت نمی‌شود.
        if (isStaffTraffic()) return;

        Visitor.FirstTouch firstTouch = new Visitor.FirstTouch();
        firstTouch.setChannel(source.channel());
        firstTouch.setSource(source.source());
        firstTouch.setMedium(source.medium());
        firstTouch.setCampaign(source.campaign());
        firstTouch.setReferrerHost(source.referrerHost());
        firstTouch.setLandingPath(source.landingPath());

        Update update = new Update()
                .setOnInsert("firstSeenAt", Instant.now())
                .setOnInsert("firstTouch", firstTouch)
                .set("lastSeenAt", Instant.now())
                .set("lastDevice", ctx.device())
                // اولین منبع دست‌نخورده می‌ماند؛ این یکی هر بار به‌روز می‌شود تا در
                // نمای «کاربران» معلوم باشد این بازدید از کجا آمده.
                .set("lastChannel", ctx.channel() == null ? Channel.DIRECT : ctx.channel())
                .set("lastCampaign", ctx.campaign());
        if (ctx.city() != null) update.set("lastCity", ctx.city());

        String userId = currentUserId();
        if (userId != null) update.set("userId", userId);

        try {
            mongo.upsert(Query.query(Criteria.where("_id").is(ctx.anonId())), update, Visitor.class);
        } catch (Exception e) {
            log.debug("آپسرتِ بازدیدکننده ناموفق بود: {}", e.toString());
        }
    }

    /**
     * دوختنِ هویت در لحظهٔ ورود/ثبت‌نامِ موفق.
     * <p>
     * رابطه یک‌به‌چند است: همین کاربر روی گوشی و لپ‌تاپ {@code anonId}های متفاوت دارد.
     * پس این متد فقط سندِ همین دستگاه را علامت می‌زند و «همهٔ شناسه‌های این کاربر»
     * یک کوئری روی {@code userId} می‌ماند.
     */
    public void identify(String userId) {
        HttpServletRequest request = currentRequest();
        if (request == null || userId == null) return;
        AnalyticsContext ctx = (AnalyticsContext) request.getAttribute(AnalyticsContext.REQUEST_ATTRIBUTE);
        if (ctx == null || ctx.anonId() == null) return;
        // ورودِ کارمند سندِ بازدیدکننده نمی‌سازد. ورودِ خودِ کارکنان جای دیگری ثبت
        // می‌شود: activity_logs، که لاگِ ممیزیِ کارکنان است.
        if (isStaffTraffic()) return;
        try {
            mongo.upsert(Query.query(Criteria.where("_id").is(ctx.anonId())),
                    new Update().set("userId", userId).set("lastSeenAt", Instant.now()),
                    Visitor.class);
        } catch (Exception e) {
            log.debug("دوختنِ هویت ناموفق بود: {}", e.toString());
        }
        record(ctx, EventType.IDENTIFY, request.getRequestURI(), "USER", userId, null, null, null);
    }

    // ==========================================================
    // تخلیهٔ صف
    // ==========================================================

    @Scheduled(fixedDelay = 2000)
    public void flush() {
        if (queue.isEmpty()) return;
        List<UserEvent> batch = new ArrayList<>(props.getFlushBatchSize());
        queue.drainTo(batch, props.getFlushBatchSize());
        if (batch.isEmpty()) return;
        try {
            mongo.insert(batch, UserEvent.class);
        } catch (Exception e) {
            // دادهٔ آماری است؛ ازدست‌رفتنِ یک دسته نباید چیزی را بشکند.
            log.warn("نوشتنِ دستهٔ رویدادها ناموفق بود ({} رویداد): {}", batch.size(), e.toString());
        }
    }

    /** برایِ تست و ممیزی — چند رویداد به‌خاطرِ پرشدنِ صف دور ریخته شده. */
    public long droppedCount() { return dropped.get(); }

    public int queueSize() { return queue.size(); }

    // ==========================================================
    // کمکی‌ها
    // ==========================================================

    /**
     * 🔴 شناسهٔ کاربر <b>همیشه</b> از {@code SecurityContext} خوانده می‌شود، هرگز از
     * بدنهٔ درخواست. اگر از بدنه خوانده می‌شد، هرکسی می‌توانست تاریخچهٔ جعلی برایِ هر
     * کاربری بسازد.
     */
    /**
     * 🔴 ترافیکِ کارکنان رفتارِ مشتری نیست و هیچ رویدادی نمی‌سازد.
     * <p>
     * خطرِ این یکی از باگ‌های معمولی بیشتر است چون <b>عددها معقول به‌نظر می‌رسند</b>،
     * فقط مالِ مشتری نیستند: کارشناسی که روزانه در فروشگاه می‌گردد
     * {@code PRODUCT_VIEW} و {@code SEARCH} و حتی {@code ADD_TO_CART} می‌سازد و در
     * جدولِ «کاربران» به‌عنوان بازدیدکننده ظاهر می‌شود. با ترافیکِ واقعیِ کمِ امروز،
     * آمارِ تیم می‌تواند بر کلِ داده غلبه کند و هر تصمیمی که از آن دربیاید غلط باشد.
     * <p>
     * و چون {@code daily_stats} هرگز پاک نمی‌شود، هر روزی که آلوده جمع‌بندی شود برای
     * همیشه آلوده می‌ماند — پس این چک باید پیش از استقرار سرِ جایش باشد.
     * <p>
     * تشخیص از همان {@code SecurityContext}ی است که {@code userId} از آن خوانده
     * می‌شود؛ فهرستِ مسیرها به‌تنهایی کافی نیست چون کارمند از مسیرهای عمومی می‌گذرد.
     */
    private boolean isStaffTraffic() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;

        // ⚠️ بازدیدکنندهٔ ناشناس در اسپرینگ «احرازشده» است و نقشِ ROLE_ANONYMOUS
        // دارد. اگر صرفاً «هر نقشی جز ROLE_USER» را کارمند حساب کنیم، همین ناشناس‌ها
        // هم کارمند شمرده می‌شوند و کلِ آمارِ مشتریان — که بیشترش ناشناس است — بی‌صدا
        // از بین می‌رود. این در تست دیده شد، وگرنه روی سرور خاموش و بی‌نشانه بود.
        if (auth instanceof AnonymousAuthenticationToken) return false;

        return auth.getAuthorities().stream()
                .map(Object::toString)
                .anyMatch(r -> r.startsWith("ROLE_")
                        && !r.equals("ROLE_USER")
                        && !r.equals("ROLE_ANONYMOUS"));
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        String username = auth.getName();
        if (username == null || "anonymousUser".equals(username)) return null;

        String cached = userIdCache.get(username);
        if (cached != null) return cached;
        try {
            String id = userRepository.findByUsername(username).map(u -> u.getId()).orElse(null);
            if (id != null) {
                if (userIdCache.size() > 10_000) userIdCache.clear();
                userIdCache.put(username, id);
            }
            return id;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * زمانِ رویداد.
     * <p>
     * 🔴 زمانِ کلاینت استفاده می‌شود ولی <b>مهار شده</b>. اگر سرور زمانِ دریافت را
     * بگذارد، بیست رویدادِ یک بستهٔ بیکن همه یک زمان می‌گیرند و ترتیبِ داخلِ بازدید از
     * بین می‌رود — یعنی دقیقاً «سفرِ کاربر» خراب می‌شود. از طرفِ دیگر ساعتِ دستگاهِ
     * کاربر قابلِ اعتماد نیست، پس زمانِ پرت به زمانِ سرور برمی‌گردد.
     */
    private Instant clampTime(Instant clientAt) {
        Instant now = Instant.now();
        if (clientAt == null) return distinct(now);
        if (clientAt.isAfter(now.plus(MAX_CLOCK_SKEW_FUTURE))) return distinct(now);
        if (clientAt.isBefore(now.minus(MAX_CLOCK_SKEW_PAST))) return distinct(now);
        return clientAt;
    }

    /**
     * تضمینِ یکتاییِ زمان برایِ رویدادهایی که سرور در یک درخواست پشتِ‌هم می‌نویسد.
     * <p>
     * 🔴 مونگو زمان را در دقتِ <b>میلی‌ثانیه</b> ذخیره می‌کند، و
     * {@code SESSION_START} و {@code PAGE_VIEW}ِ یک درخواست عملاً همیشه در یک
     * میلی‌ثانیه می‌افتند. آن‌وقت مرتب‌سازی بر اساسِ {@code at} ترتیبشان را دلبخواهی
     * نشان می‌دهد — یعنی «سفرِ کاربر» که خواستهٔ اصلیِ مالک است بی‌ترتیب می‌شود.
     * <p>
     * به‌جایِ افزودنِ فیلدِ ترتیب (که هر مصرف‌کننده باید یادش باشد در مرتب‌سازی بیاوردش)،
     * زمان حداقل یک میلی‌ثانیه جلو می‌رود تا {@code at} به‌تنهایی کلیدِ مرتب‌سازیِ درستی
     * بماند. اگر جلورفتن از یک ثانیه بیشتر شود (بارِ خیلی سنگین) رها می‌شود و زمانِ
     * واقعی برمی‌گردد: درستیِ زمان مهم‌تر از ترتیبِ رویدادهایِ کاربرانِ متفاوت است.
     */
    private Instant distinct(Instant rawNow) {
        // ⚠️ در همان دقتی مقایسه می‌شود که مونگو ذخیره می‌کند. Instant.now() دقتِ
        // میکروثانیه دارد، پس دو رویدادِ پشتِ‌هم «متفاوت» به‌نظر می‌رسند ولی بعد از
        // گردشدن به میلی‌ثانیه یکی می‌شوند — که همان باگی بود که اول خوردیم.
        Instant now = rawNow.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        while (true) {
            Instant previous = lastIssuedAt.get();
            Instant candidate = (previous == null || now.isAfter(previous))
                    ? now : previous.plusMillis(1);
            if (candidate.isAfter(now.plusMillis(1000))) return now;
            if (lastIssuedAt.compareAndSet(previous, candidate)) return candidate;
        }
    }

    /** سقفِ تعداد کلید و حجم — بدونِ آن، اولین باتی که بیکن را پیدا کند دیتابیس را پر می‌کند. */
    private Map<String, Object> limitProps(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) return null;
        Map<String, Object> out = new LinkedHashMap<>();
        int bytes = 0;
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            if (out.size() >= props.getMaxPropKeys()) break;
            String key = trim(entry.getKey(), 40);
            if (key == null || key.isBlank()) continue;
            Object value = entry.getValue();
            if (value instanceof String s) value = trim(s, 200);
            String rendered = String.valueOf(value);
            bytes += key.length() + rendered.length();
            if (bytes > props.getMaxPropBytes()) break;
            out.put(key, value);
        }
        return out.isEmpty() ? null : out;
    }

    private String trim(String value, int max) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;
        return v.length() > max ? v.substring(0, max) : v;
    }

    private HttpServletRequest currentRequest() {
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            return attrs instanceof ServletRequestAttributes sra ? sra.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
