package org.example.shop1.model.service;

import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.ActivityLog;
import org.example.shop1.model.entity.Counter;
import com.mongodb.client.result.UpdateResult;
import org.example.shop1.model.entity.HolooCode;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.reposritory.HolooCodeRepository;
import org.example.shop1.model.reposritory.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
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
    private String nextFreeCode() {
        for (int attempt = 0; attempt < 50; attempt++) {
            String code = format(nextSequence());
            if (codeRepo.existsByCode(code) || productRepo.existsByHolooCode(code)) {
                log.warn("کدِ {} از قبل استفاده شده — شمارهٔ بعدی گرفته می‌شود", code);
                continue;
            }
            return code;
        }
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                "صدورِ کدِ تازه ناموفق بود — شمارنده را بررسی کنید");
    }

    private HolooCode issue(HolooCode.Status status, String note, String productId) {
        HolooCode entry = new HolooCode(nextFreeCode(), status, note, activityLog.currentUsername());
        if (status == HolooCode.Status.ASSIGNED) {
            entry.setProductId(productId);
            entry.setAssignedBy(activityLog.currentUsername());
            entry.setAssignedAt(Instant.now());
        }
        return codeRepo.save(entry);
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

    /**
     * صدورِ کد برایِ محصولی که <b>هنوز کد ندارد</b> — عملِ صریحِ جدا، نه بخشی از ذخیره.
     * <p>
     * 🔴 قاعدهٔ «ذخیره کد صادر نمی‌کند» دو چیزِ متفاوت را با هم بسته بود: عوض‌کردنِ کدِ
     * موجود (که باید ممنوع بماند، چون شرکت همان را در هلو تایپ کرده) و پرکردنِ کدِ
     * خالی (که باید ممکن باشد، وگرنه کالا در هیچ همگام‌سازی‌ای پیدا نمی‌شود). این متد
     * فقط دومی را باز می‌کند، و چون مسیرِ جداست، ویرایشِ محصول همچنان هیچ کدی نمی‌سوزاند.
     *
     * @param requestedCode کدِ رزروشدهٔ انتخاب‌شده، یا نال/خالی برایِ کدِ تازه
     */
    public String assignToExistingProduct(String productId, String requestedCode) {
        Product product = productRepo.findById(productId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "محصول پیدا نشد: " + productId));

        if (product.getHolooCode() != null && !product.getHolooCode().isBlank()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "این محصول از قبل کدِ «" + product.getHolooCode() + "» را دارد و کد عوض نمی‌شود.");
        }

        String wanted = requestedCode == null ? "" : requestedCode.trim();
        HolooCode reservation = null;
        String code;

        if (wanted.isEmpty()) {
            code = nextFreeCode();
        } else {
            reservation = codeRepo.findByCode(wanted).orElseThrow(() -> new ApiException(
                    HttpStatus.BAD_REQUEST, "کدِ «" + wanted + "» صادر نشده است"));
            if (reservation.getStatus() != HolooCode.Status.RESERVED) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "کدِ «" + wanted + "» رزروِ مصرف‌نشده نیست — قبلاً روی محصولی نشسته است");
            }
            code = reservation.getCode();
        }

        claimCodeSlot(productId, code);

        // دفتر بعد از قفل‌شدنِ جایِ محصول نوشته می‌شود: اگر مسابقه را ببازیم،
        // دفتر آلوده نمی‌ماند.
        if (reservation != null) {
            reservation.setStatus(HolooCode.Status.ASSIGNED);
            reservation.setProductId(productId);
            reservation.setAssignedBy(activityLog.currentUsername());
            reservation.setAssignedAt(Instant.now());
            codeRepo.save(reservation);
        } else {
            HolooCode entry = new HolooCode(code, HolooCode.Status.ASSIGNED, null,
                    activityLog.currentUsername());
            entry.setProductId(productId);
            entry.setAssignedBy(activityLog.currentUsername());
            entry.setAssignedAt(Instant.now());
            codeRepo.save(entry);
        }

        activityLog.record(ActivityLog.Action.HOLOO_CODE_ISSUE, ActivityLog.Source.MANUAL,
                ActivityLogService.ENTITY_PRODUCT, productId, product.getName(),
                "holooCode", null, code);
        return code;
    }

    /**
     * نشاندنِ کد روی محصول با یک نوشتنِ شرطی — «فقط اگر هنوز کد ندارد».
     * <p>
     * 🔴 <b>چرا ایندکسِ یکتا اینجا کافی نیست:</b> ایندکس جلویِ «دو محصول، یک کد» را
     * می‌گیرد، ولی مسابقهٔ واقعیِ این مسیر «یک محصول، دو کد» است — دو درخواستِ هم‌زمان
     * از شمارندهٔ اتمیک دو شمارهٔ <i>متفاوت</i> می‌گیرند، پس هیچ کلیدِ تکراری‌ای رخ
     * نمی‌دهد و دومی بی‌صدا اولی را بازمی‌نویسد. شرطِ {@code holooCode == null} داخلِ
     * خودِ کوئری این را اتمیک می‌بندد: بازنده {@code modifiedCount == 0} می‌گیرد.
     * <p>
     * ایندکسِ یکتا همچنان لازم است و {@code DuplicateKeyException}ش اینجا به پیامِ
     * روشن ترجمه می‌شود — آن حالتِ دیگر (دو محصول، یک کدِ رزروشده) را می‌بندد.
     */
    private void claimCodeSlot(String productId, String code) {
        UpdateResult result;
        try {
            result = mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(productId).and("holooCode").is(null)),
                    new Update().set("holooCode", code),
                    Product.class);
        } catch (DuplicateKeyException e) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "کدِ «" + code + "» همین حالا روی محصولِ دیگری نشست — دوباره تلاش کنید");
        }
        if (result.getModifiedCount() == 0) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "کدِ این محصول همین حالا به‌دستِ درخواستِ دیگری ست شد — فرم را ببندید و دوباره باز کنید");
        }
    }

    // ==========================================================
    // مهاجرتِ یک‌باره
    // ==========================================================

    /**
     * @param reservedConsumed چند ردیف کدی را نشاندند که تا آن لحظه {@code RESERVED} بود.
     *                         بدونِ این عدد، مصرفِ رزروِ کسِ دیگر در مهاجرت بی‌صدا بود.
     * @param reservedDetail   شرحِ همان‌ها، به‌علاوهٔ کدهایی که از محصولی به محصولِ دیگر رفتند.
     */
    public record MigrationResult(int rows, int assigned, int alreadyCorrect, int missingProduct,
                                  int conflicts, List<String> conflictDetail, List<String> missingDetail,
                                  int reservedConsumed, List<String> reservedDetail,
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
        List<String> reservedDetail = new java.util.ArrayList<>();
        int reservedConsumed = 0;

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
                String notice = recordExternal(code, product.getId());   // دفتر هم بی‌اثر‌پذیر پر می‌شود
                if (notice != null) {
                    reservedConsumed++;
                    reservedDetail.add(notice);
                }
                continue;
            }
            if (current != null && !current.isBlank()) {
                conflicts++;
                conflictDetail.add(product.getId() + ": «" + current + "» ≠ «" + code + "»");
                continue;
            }
            product.setHolooCode(code);
            productRepo.save(product);
            String notice = recordExternal(code, product.getId());
            if (notice != null) {
                reservedConsumed++;
                reservedDetail.add(notice);
            }
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
                conflictDetail, missingDetail, reservedConsumed, reservedDetail,
                before, after, prefix());
    }

    /**
     * ثبتِ کدی که از بیرون آمده (مهاجرتِ یک‌باره) در دفتر — بی‌اثر‌پذیر.
     * <p>
     * شمارنده را جلو <b>نمی‌برد</b>؛ آن کارِ فراخوانِ مهاجرت است که بعد از تمامِ
     * ردیف‌ها یک‌بار {@link #raiseSequenceTo(long)} می‌زند.
     */
    public String recordExternal(String code, String productId) {
        HolooCode existing = codeRepo.findByCode(code).orElse(null);
        String notice = null;

        if (existing == null) {
            existing = new HolooCode(code, HolooCode.Status.ASSIGNED, "مهاجرتِ یک‌باره",
                    activityLog.currentUsername());
        } else if (existing.getStatus() == HolooCode.Status.RESERVED) {
            // 🔴 رزروی که مهاجرت مصرفش می‌کند. ممنوع نیست — همین مسیر امروز تنها راهِ
            // نشاندنِ کد روی محصولِ موجود است — ولی نباید بی‌صدا باشد: کسی این کد را
            // گرفته و شاید همین حالا در هلو روی کالایی تایپش کرده.
            notice = "رزروِ «" + code + "» مصرف شد"
                    + (existing.getNote() == null || existing.getNote().isBlank()
                    ? "" : " (یادداشت: " + existing.getNote() + ")");
        } else if (existing.getProductId() != null && !existing.getProductId().equals(productId)) {
            // کدی که از قبل روی محصولِ دیگری نشسته بود و حالا جابه‌جا می‌شود
            notice = "کدِ «" + code + "» از محصولِ " + existing.getProductId()
                    + " به " + productId + " منتقل شد";
        }

        existing.setStatus(HolooCode.Status.ASSIGNED);
        existing.setProductId(productId);
        if (existing.getAssignedAt() == null) existing.setAssignedAt(Instant.now());
        codeRepo.save(existing);
        return notice;
    }
}
