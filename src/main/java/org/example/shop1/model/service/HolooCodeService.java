package org.example.shop1.model.service;

import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.ActivityLog;
import org.example.shop1.model.entity.Counter;
import org.example.shop1.model.entity.HolooCode;
import org.example.shop1.model.reposritory.HolooCodeRepository;
import org.example.shop1.model.reposritory.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * صدور و رزروِ کدِ کالا — همان کدی که شرکت در نرم‌افزارِ حسابداری (هلو) روی کالا
 * می‌نشاند تا همگام‌سازیِ موجودی به‌جایِ حدسِ نام، تطبیقِ قطعی داشته باشد.
 *
 * <h3>دو جریانِ متفاوت</h3>
 * <ul>
 *   <li><b>محصول‌اول:</b> کارشناس محصول را می‌سازد، کد خودکار صادر می‌شود.</li>
 *   <li><b>کد‌اول:</b> کارشناس کدی می‌گیرد تا همین حالا در هلو ثبت کند، و بعداً
 *       محصولش را با همان کدِ رزروشده می‌سازد.</li>
 * </ul>
 * هر دو از یک شمارنده تغذیه می‌شوند، وگرنه دو جریان به‌هم می‌رسند.
 *
 * <h3>🔴 پیشوند در کد هاردکد نیست</h3>
 * پیشوندِ {@code DN-} مخصوصِ همین فروشگاه است و در {@code store_settings} می‌نشیند،
 * نه در این کلاس. نصبِ تازه با پیشوندِ خنثایِ {@link #FALLBACK_PREFIX} کار می‌کند و
 * مهاجرتِ یک‌باره پیشوند را از خودِ دادهٔ واردشده یاد می‌گیرد.
 */
@Service
public class HolooCodeService {

    private static final Logger log = LoggerFactory.getLogger(HolooCodeService.class);

    /** نامِ دنباله در کالکشنِ {@code counters}. */
    public static final String COUNTER_NAME = "holooCode";

    /** پیشوندِ خنثی وقتی فروشگاه هنوز پیشوندِ خودش را تنظیم نکرده. */
    public static final String FALLBACK_PREFIX = "SKU-";

    /** تعدادِ ارقامِ پیش‌فرض — چهار رقم تا ۹۹۹۹ کالا جا بدهد. */
    public static final int FALLBACK_DIGITS = 4;

    /** الگویِ عمومیِ یک کد: پیشوند + رقم. برایِ تشخیصِ ستونِ کد در فایلِ اکسل هم همین است. */
    private static final Pattern CODE_PATTERN = Pattern.compile("^([A-Za-z][A-Za-z0-9]*-)(\\d{3,8})$");

    private final MongoTemplate mongoTemplate;
    private final HolooCodeRepository codeRepo;
    private final ProductRepository productRepo;
    private final StoreSettingsService settingsService;
    private final ActivityLogService activityLog;

    public HolooCodeService(MongoTemplate mongoTemplate, HolooCodeRepository codeRepo,
                            ProductRepository productRepo, StoreSettingsService settingsService,
                            ActivityLogService activityLog) {
        this.mongoTemplate = mongoTemplate;
        this.codeRepo = codeRepo;
        this.productRepo = productRepo;
        this.settingsService = settingsService;
        this.activityLog = activityLog;
    }

    // ==========================================================
    // شمارنده
    // ==========================================================

    /**
     * شمارهٔ بعدی — اتمیک.
     * <p>
     * {@code findAndModify} با {@code $inc} در مونگو یک عملیاتِ تکِ اتمیک است، پس دو
     * درخواستِ هم‌زمان قطعاً دو عدد می‌گیرند. {@code upsert} یعنی اولین فراخوانی روی
     * دیتابیسِ خالی هم کار می‌کند.
     */
    public long nextSequence() {
        Counter c = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(COUNTER_NAME)),
                new Update().inc("seq", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                Counter.class);
        return c == null ? 1L : c.getSeq();
    }

    /** مقدارِ فعلیِ شمارنده بدونِ افزایش — برایِ گزارش و ممیزی. */
    public long currentSequence() {
        Counter c = mongoTemplate.findById(COUNTER_NAME, Counter.class);
        return c == null ? 0L : c.getSeq();
    }

    /**
     * شمارنده را دستِ‌کم تا {@code value} جلو می‌برد. هرگز عقب نمی‌برد.
     * <p>
     * 🔴 این همان چیزی است که بعد از مهاجرتِ یک‌باره باید صدا زده شود. اگر فراموش
     * شود، محصولِ بعدی {@code DN-0001} می‌گیرد و با محصولی که همین کد را دارد تصادم
     * می‌کند — باگی بی‌صدا که تا اولین همگام‌سازیِ موجودی معلوم نمی‌شود. عقب‌نرفتن
     * هم عمدی است: شمارنده فقط جلو می‌رود، وگرنه کدِ سوخته دوباره صادر می‌شود.
     */
    public long raiseSequenceTo(long value) {
        long current = currentSequence();
        if (value <= current) return current;
        mongoTemplate.upsert(
                Query.query(Criteria.where("_id").is(COUNTER_NAME)),
                new Update().set("seq", value),
                Counter.class);
        log.info("شمارندهٔ کدِ کالا از {} به {} برده شد", current, value);
        return value;
    }

    // ==========================================================
    // قالبِ کد
    // ==========================================================

    public String prefix() {
        String configured = settingsService.getSettings().getHolooCodePrefix();
        return (configured == null || configured.isBlank()) ? FALLBACK_PREFIX : configured.trim();
    }

    public int digits() {
        Integer configured = settingsService.getSettings().getHolooCodeDigits();
        return (configured == null || configured < 3 || configured > 8) ? FALLBACK_DIGITS : configured;
    }

    public String format(long sequence) {
        return prefix() + String.format("%0" + digits() + "d", sequence);
    }

    /** آیا این رشته اصلاً شکلِ یک کد را دارد؟ (بدونِ کاری به اینکه صادر شده یا نه) */
    public static boolean looksLikeCode(String value) {
        return value != null && CODE_PATTERN.matcher(value.trim()).matches();
    }

    /** شمارهٔ داخلِ کد، یا {@code -1} اگر کد نباشد. */
    public static long sequenceOf(String code) {
        if (code == null) return -1;
        Matcher m = CODE_PATTERN.matcher(code.trim());
        if (!m.matches()) return -1;
        try {
            return Long.parseLong(m.group(2));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** پیشوندِ داخلِ کد (مثلاً {@code DN-})، یا نال. */
    public static String prefixOf(String code) {
        if (code == null) return null;
        Matcher m = CODE_PATTERN.matcher(code.trim());
        return m.matches() ? m.group(1) : null;
    }

    // ==========================================================
    // صدور
    // ==========================================================

    /**
     * یک کدِ تازه از شمارنده می‌گیرد و در دفتر ثبتش می‌کند.
     * <p>
     * ⚠️ حلقهٔ تلاشِ مجدد عمدی است: اگر کدی با همین شماره از قبل در دفتر باشد
     * (مثلاً مهاجرت کدی را نشانده ولی شمارنده هنوز جلو نرفته)، شمارهٔ بعدی گرفته
     * می‌شود به‌جایِ اینکه ایندکسِ یکتا با خطای خام بترکد.
     */
    private HolooCode issue(HolooCode.Status status, String note, String productId) {
        for (int attempt = 0; attempt < 50; attempt++) {
            long seq = nextSequence();
            String code = format(seq);
            if (codeRepo.existsByCode(code) || productRepo.existsByHolooCode(code)) {
                log.warn("کدِ {} از قبل استفاده شده — شمارهٔ بعدی گرفته می‌شود", code);
                continue;
            }
            HolooCode entry = new HolooCode(code, status, note, activityLog.currentUsername());
            if (status == HolooCode.Status.ASSIGNED) {
                entry.setProductId(productId);
                entry.setAssignedBy(activityLog.currentUsername());
                entry.setAssignedAt(Instant.now());
            }
            return codeRepo.save(entry);
        }
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                "صدورِ کدِ تازه ناموفق بود — شمارنده را بررسی کنید");
    }

    /**
     * «دریافتِ کدِ هلو» — جریانِ کد‌اول. کد صادر و رزرو می‌شود تا کارشناس همین حالا
     * در هلو ثبتش کند و بعداً محصولش را با همین کد بسازد.
     */
    public HolooCode reserve(String note) {
        String clean = note == null ? null : note.trim();
        if (clean == null || clean.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "یادداشت لازم است — بنویسید این کد برای چه کالایی است");
        }
        HolooCode saved = issue(HolooCode.Status.RESERVED, clean, null);
        activityLog.record(ActivityLog.Action.HOLOO_CODE_ISSUE, ActivityLog.Source.MANUAL,
                ActivityLogService.ENTITY_PRODUCT, null, clean, "holooCode", null, saved.getCode());
        return saved;
    }

    /** رزروهایِ هنوز مصرف‌نشده — جریانِ «کد گرفتم و یادم رفت» بدونِ این دیده نمی‌شود. */
    public List<HolooCode> openReservations() {
        return codeRepo.findByStatusOrderByIssuedAtDesc(HolooCode.Status.RESERVED);
    }

    /**
     * کدِ محصولِ تازه — یا کدِ رزروشده‌ای که کارشناس انتخاب کرده، یا یک کدِ تازه.
     * <p>
     * 🔴 فقط از مسیرِ <b>ساخت</b> صدا زده می‌شود. اگر در مسیرِ ذخیرهٔ عمومی می‌نشست،
     * هر ویرایشِ یک محصولِ موجود یک کد می‌سوزاند.
     *
     * @param requestedCode کدِ رزروشده‌ای که کارشناس انتخاب کرده، یا نال/خالی برایِ کدِ تازه
     */
    public String assignForNewProduct(String productId, String requestedCode) {
        String wanted = requestedCode == null ? "" : requestedCode.trim();

        if (wanted.isEmpty()) {
            HolooCode fresh = issue(HolooCode.Status.ASSIGNED, null, productId);
            activityLog.record(ActivityLog.Action.HOLOO_CODE_ISSUE, ActivityLog.Source.MANUAL,
                    ActivityLogService.ENTITY_PRODUCT, productId, null, "holooCode", null, fresh.getCode());
            return fresh.getCode();
        }

        HolooCode reserved = codeRepo.findByCode(wanted).orElseThrow(() -> new ApiException(
                HttpStatus.BAD_REQUEST, "کدِ «" + wanted + "» صادر نشده است"));
        if (reserved.getStatus() == HolooCode.Status.ASSIGNED) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "کدِ «" + wanted + "» قبلاً روی محصولِ دیگری نشسته است");
        }
        reserved.setStatus(HolooCode.Status.ASSIGNED);
        reserved.setProductId(productId);
        reserved.setAssignedBy(activityLog.currentUsername());
        reserved.setAssignedAt(Instant.now());
        codeRepo.save(reserved);
        activityLog.record(ActivityLog.Action.HOLOO_CODE_ISSUE, ActivityLog.Source.MANUAL,
                ActivityLogService.ENTITY_PRODUCT, productId, reserved.getNote(),
                "holooCode", "رزروشده", reserved.getCode());
        return reserved.getCode();
    }

    // ==========================================================
    // مهاجرتِ یک‌باره
    // ==========================================================

    public record MigrationResult(int rows, int assigned, int alreadyCorrect, int missingProduct,
                                  int conflicts, List<String> conflictDetail, List<String> missingDetail,
                                  long counterBefore, long counterAfter, String prefix) {}

    /**
     * نشاندنِ کدهایِ از پیش‌توافق‌شده روی محصولات — <b>بی‌اثر‌پذیر</b>.
     * <p>
     * این CSV به شرکت هم داده شده و کدهایش قطعی‌اند، پس هیچ ردیفی کدِ موجود را عوض
     * نمی‌کند: اگر محصول کدی <i>دیگر</i> داشته باشد، تعارض گزارش می‌شود و دست نمی‌خورد.
     * تطبیق با {@code productId} است نه نام — دقیقاً همان چیزی که کلِ این کار برایِ
     * فرارِ از آن انجام می‌شود.
     * <p>
     * 🔴 در پایان شمارنده تا بزرگ‌ترین شمارهٔ واردشده جلو می‌رود. بدونِ این، محصولِ
     * بعدی {@code DN-0001} می‌گیرد و با محصولی که همین کد را دارد تصادم می‌کند —
     * بی‌صداترین باگِ ممکن، چون تا اولین همگام‌سازی معلوم نمی‌شود.
     */
    public MigrationResult migrateFromCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "محتوای CSV خالی است");
        }
        long before = currentSequence();
        int rows = 0, assigned = 0, already = 0, missing = 0, conflicts = 0;
        long maxSeq = 0;
        String seenPrefix = null;
        List<String> conflictDetail = new java.util.ArrayList<>();
        List<String> missingDetail = new java.util.ArrayList<>();

        for (String rawLine : csv.split("\\r?\\n")) {
            String line = rawLine.replace("\uFEFF", "").trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(",", 3);
            if (parts.length < 2) continue;
            String code = parts[0].trim();
            String productId = parts[1].trim();
            if (!looksLikeCode(code)) continue;   // سرستون و خطوطِ توضیحی

            rows++;
            maxSeq = Math.max(maxSeq, sequenceOf(code));
            if (seenPrefix == null) seenPrefix = prefixOf(code);

            var found = productRepo.findById(productId);
            if (found.isEmpty()) {
                missing++;
                missingDetail.add(code + " → " + productId);
                continue;
            }
            org.example.shop1.model.entity.Product product = found.get();
            String current = product.getHolooCode();
            if (code.equals(current)) {
                already++;
                recordExternal(code, product.getId());   // دفتر هم بی‌اثر‌پذیر پر می‌شود
                continue;
            }
            if (current != null && !current.isBlank()) {
                conflicts++;
                conflictDetail.add(product.getId() + ": «" + current + "» ≠ «" + code + "»");
                continue;
            }
            product.setHolooCode(code);
            productRepo.save(product);
            recordExternal(code, product.getId());
            assigned++;
        }

        // پیشوند را از خودِ داده یاد می‌گیریم، نه از کد — تا در تنظیمات بنشیند نه در هسته
        if (seenPrefix != null) {
            var settings = settingsService.getSettings();
            if (settings.getHolooCodePrefix() == null || settings.getHolooCodePrefix().isBlank()) {
                settings.setHolooCodePrefix(seenPrefix);
                settingsService.updateSettings(settings);
            }
        }

        long after = raiseSequenceTo(maxSeq);
        activityLog.record(ActivityLog.Action.HOLOO_CODE_ISSUE, ActivityLog.Source.BATCH,
                ActivityLogService.ENTITY_PRODUCT, null, "مهاجرتِ یک‌بارهٔ کدِ کالا", "holooCode",
                "شمارنده " + before, "نشانده " + assigned + " · از قبل درست " + already
                        + " · شمارنده " + after);

        return new MigrationResult(rows, assigned, already, missing, conflicts,
                conflictDetail, missingDetail, before, after, prefix());
    }

    /**
     * ثبتِ کدی که از بیرون آمده (مهاجرتِ یک‌باره) در دفتر — بی‌اثر‌پذیر.
     * <p>
     * شمارنده را جلو <b>نمی‌برد</b>؛ آن کارِ فراخوانِ مهاجرت است که بعد از تمامِ
     * ردیف‌ها یک‌بار {@link #raiseSequenceTo(long)} می‌زند.
     */
    public void recordExternal(String code, String productId) {
        HolooCode entry = codeRepo.findByCode(code).orElseGet(() -> new HolooCode(
                code, HolooCode.Status.ASSIGNED, "مهاجرتِ یک‌باره", activityLog.currentUsername()));
        entry.setStatus(HolooCode.Status.ASSIGNED);
        entry.setProductId(productId);
        if (entry.getAssignedAt() == null) entry.setAssignedAt(Instant.now());
        codeRepo.save(entry);
    }
}
