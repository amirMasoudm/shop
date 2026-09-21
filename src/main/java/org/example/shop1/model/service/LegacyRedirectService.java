package org.example.shop1.model.service;

import org.bson.Document;
import org.example.shop1.model.entity.LegacyRedirect;
import org.example.shop1.model.reposritory.LegacyRedirectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * ریدایرکتِ آدرس‌های قدیمیِ ایندکس‌شده.
 *
 * <h3>🔴 نرمال‌سازیِ کلید — قلبِ این سرویس</h3>
 * آدرس‌های وردپرسِ فارسی درصد-کدشده ذخیره شده‌اند ({@code %d8%b9%db%8c…}) و گوگل
 * ممکن است همان را با حروفِ بزرگ، یا دیکدشده، یا بدونِ اسلشِ پایانی، یا با دنبالهٔ
 * {@code ?utm_source=…} بفرستد. اگر کلید هنگامِ ذخیره و هنگامِ جست‌وجو یکسان نرمال
 * نشود، بخشِ بزرگی از آدرس‌ها <b>در عمل پیدا نمی‌شوند</b> — و این خطا بی‌صداست:
 * چیزی خراب به‌نظر نمی‌رسد، فقط ۴۰۴ می‌گیریم.
 *
 * <h3>چرا شمارنده غیرهمگام است</h3>
 * هر برخورد یک نوشتن در دیتابیس بود یعنی ربات‌های خزنده می‌توانستند با درخواستِ
 * پشتِ سرهم صفحهٔ ۴۰۴ را به بارِ نوشتن تبدیل کنند. شمارش در حافظه جمع می‌شود و
 * دوره‌ای یک‌جا می‌نشیند — همان الگوی صفِ ردگیری.
 */
@Service
public class LegacyRedirectService {

    private static final Logger log = LoggerFactory.getLogger(LegacyRedirectService.class);

    private static final String COLLECTION = "legacy_redirects";
    /** سقفِ حافظهٔ شمارنده — مسیرهای بی‌ربط نباید بی‌انتها انباشته شوند. */
    private static final int MAX_PENDING_KEYS = 5_000;

    private final LegacyRedirectRepository repo;
    private final MongoTemplate mongo;

    private final Map<String, LongAdder> pendingHits = new ConcurrentHashMap<>();

    /**
     * کلیدهای موجود، در حافظه.
     * <p>
     * 🔴 <b>چرا کش لازم است:</b> تشخیصِ «این مسیر ریدایرکت دارد» باید <b>پیش از</b>
     * زنجیرهٔ امنیت انجام شود، وگرنه آدرسِ قدیمی به‌جای ۳۰۱، ۴۰۱ می‌گیرد. یعنی روی
     * <b>هر</b> درخواست. یک کوئریِ دیتابیس به‌ازای هر درخواست پذیرفتنی نیست؛ چند ده
     * رشته در حافظه هست.
     */
    private volatile Set<String> knownPaths = Set.of();

    public LegacyRedirectService(LegacyRedirectRepository repo, MongoTemplate mongo) {
        this.repo = repo;
        this.mongo = mongo;
    }

    @jakarta.annotation.PostConstruct
    public void reloadKeys() {
        try {
            Set<String> next = new HashSet<>();
            for (LegacyRedirect r : repo.findAll()) {
                if (r.getFromPath() != null) next.add(r.getFromPath());
            }
            knownPaths = Set.copyOf(next);
            log.info("کلیدِ ریدایرکتِ قدیمی در حافظه: {}", knownPaths.size());
        } catch (Exception e) {
            log.warn("بارگذاریِ کلیدهای ریدایرکت ناموفق بود: {}", e.toString());
        }
    }

    /** آیا اصلاً ارزشِ خواندن از دیتابیس را دارد؟ */
    public boolean mightRedirect(String normalizedPath) {
        return knownPaths.contains(normalizedPath);
    }

    /**
     * مسیرهایی که هرگز نباید مبدأِ ریدایرکت شوند.
     * <p>
     * 🔴 این نگهبانِ اصلیِ «هندلر جلوی مسیرهای واقعیِ اپ را نگیرد» است. چون تشخیص
     * پیش از مسیریابیِ اسپرینگ انجام می‌شود، دیگر نمی‌شود به «اول بگذار اپ تلاش کند»
     * تکیه کرد — پس ورودی در لحظهٔ <b>ساخت</b> بسته می‌شود، نه در لحظهٔ اجرا.
     */
    public static boolean isReservedPath(String normalizedPath) {
        if (normalizedPath == null || normalizedPath.equals("/")) return true;
        if (normalizedPath.endsWith(".html")) return true;
        for (String prefix : RESERVED_PREFIXES) {
            if (normalizedPath.equals(prefix) || normalizedPath.startsWith(prefix + "/")) return true;
        }
        return false;
    }

    private static final List<String> RESERVED_PREFIXES = List.of(
            "/api", "/l", "/shop", "/blog", "/learn", "/about", "/wimaxnear", "/profile",
            "/css", "/js", "/img", "/images", "/fonts", "/uploads", "/fragments",
            "/actuator", "/error", "/robots.txt", "/sitemap.xml", "/favicon.ico");

    // ==========================================================
    // نرمال‌سازی
    // ==========================================================

    /**
     * یک مسیر را به کلیدِ یکتا تبدیل می‌کند.
     * <p>
     * ترتیبِ قدم‌ها مهم است: اول کوئری و قطعه دور ریخته می‌شود، بعد دیکد، بعد
     * کوچک‌سازی. اگر اول کوچک می‌کردیم، {@code %D8%B9} و {@code %d8%b9} هنوز دو
     * رشتهٔ متفاوت می‌ماندند در حالی که یک حرف‌اند.
     * <p>
     * ⚠️ دیکد داخلِ {@code try/catch} است: مسیرِ خرابِ {@code %ZZ} از بیرون می‌آید و
     * نباید ۵۰۰ بدهد. در آن حالت رشتهٔ خام کلید می‌شود — پیدا نمی‌شود و ۴۰۴ سالم می‌گیرد.
     */
    public static String normalize(String rawPath) {
        if (rawPath == null) return "/";
        String p = rawPath.trim();

        // آدرسِ کامل هم پذیرفته می‌شود تا واردکردن از فایلِ نگاشت مستقیم کار کند
        if (p.startsWith("http://") || p.startsWith("https://")) {
            try {
                String path = URI.create(p).getRawPath();
                if (path != null) p = path;
            } catch (Exception e) {
                int slash = p.indexOf('/', p.indexOf("//") + 2);
                p = slash >= 0 ? p.substring(slash) : "/";
            }
        }

        int cut = p.indexOf('?');
        if (cut >= 0) p = p.substring(0, cut);
        cut = p.indexOf('#');
        if (cut >= 0) p = p.substring(0, cut);

        try {
            p = URLDecoder.decode(p, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // مسیرِ درصد-کدشدهٔ خراب؛ خامش می‌ماند و نتیجه ۴۰۴ سالم است
        }

        p = p.toLowerCase(Locale.ROOT);
        if (!p.startsWith("/")) p = "/" + p;
        while (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return p;
    }

    /**
     * مسیرِ مقصد را برایِ هدرِ {@code Location} آماده می‌کند.
     * <p>
     * 🔴 <b>این را در تستِ واقعی گرفتم، نه در کدخوانی:</b> مقصدها فارسی‌اند
     * ({@code /blog/عیبیابی-میکروتیک-…}) و هدرِ HTTP نویسهٔ غیرِ لاتین-۱ نمی‌پذیرد.
     * تامکت هدر را <b>بی‌صدا حذف می‌کرد</b> — پاسخ ۳۰۱ بود ولی بدونِ {@code Location}،
     * یعنی مرورگر هیچ‌جا نمی‌رفت و گوگل هم ریدایرکت را نمی‌دید. هیچ خطایی هم در لاگ
     * نبود. پس هر بخشِ مسیر درصد-کد می‌شود، و جداکننده‌ها دست‌نخورده می‌مانند.
     */
    public static String toLocationHeader(String toPath) {
        if (toPath == null || toPath.isEmpty()) return "/";
        String path = toPath;
        String query = "";
        int q = path.indexOf('?');
        if (q >= 0) {
            query = path.substring(q);
            path = path.substring(0, q);
        }
        // ⚠️ با split جداکننده‌ها را خودمان برمی‌گردانیم، و بندِ اولِ خالی (همان اسلشِ
        // ابتدایی) باید بماند — بارِ اول همین افتاد و Location نسبی شد: مرورگر
        // «blog/…» را به انتهای آدرسِ قدیمی چسباند و به مسیرِ بی‌معنی رفت.
        String[] segments = path.split("/", -1);
        StringBuilder sb = new StringBuilder(path.length() * 2);
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) sb.append('/');
            sb.append(URLEncoder.encode(segments[i], StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return sb + query;
    }

    // ==========================================================
    // یافتن
    // ==========================================================

    public Optional<LegacyRedirect> resolve(String rawPath) {
        return repo.findByFromPath(normalize(rawPath));
    }

    /** شمارشِ برخورد — در حافظه، بدونِ کندکردنِ پاسخ. */
    public void countHit(String fromPath) {
        if (pendingHits.size() >= MAX_PENDING_KEYS && !pendingHits.containsKey(fromPath)) return;
        pendingHits.computeIfAbsent(fromPath, k -> new LongAdder()).increment();
    }

    @Scheduled(fixedDelayString = "${app.legacy-redirects.flush-seconds:30}000")
    public void flushHits() {
        if (pendingHits.isEmpty()) return;
        List<String> keys = new ArrayList<>(pendingHits.keySet());
        Instant now = Instant.now();
        for (String key : keys) {
            LongAdder adder = pendingHits.remove(key);
            if (adder == null) continue;
            long n = adder.sum();
            if (n == 0) continue;
            try {
                mongo.getCollection(COLLECTION).updateOne(
                        new Document("fromPath", key),
                        new Document("$inc", new Document("hits", n))
                                .append("$set", new Document("lastHitAt", java.util.Date.from(now))));
            } catch (Exception e) {
                log.debug("ثبتِ شمارشِ ریدایرکتِ {} ناموفق بود: {}", key, e.toString());
            }
        }
    }

    // ==========================================================
    // واردکردن
    // ==========================================================

    /** یک ردیفِ نگاشت، پیش از اعتبارسنجی. */
    public record MappingRow(String fromPath, String toPath, String note) {}

    /**
     * ثبتِ یک ریدایرکت با صاف‌کردنِ زنجیره و حذفِ حلقه.
     * <p>
     * 🔴 <b>چرا جدا از {@link #importRows}:</b> برخلافِ {@code ProductRedirectService}
     * که هنگامِ خواندن زنجیره را با {@code MAX_HOPS} دنبال می‌کند، مسیرِ ریدایرکتِ
     * قدیمی فقط یک {@code findByFromPath} می‌زند و <b>هیچ پرشی را دنبال نمی‌کند</b>.
     * پس زنجیره باید در <b>زمانِ نوشتن</b> صاف شود، وگرنه
     * {@code /blog/A → /blog/B → /blog/C} یعنی {@code A} به صفحه‌ای می‌رسد که خودش
     * ۳۰۱ می‌دهد — و گوگل آن را یک پرشِ اضافه می‌بیند.
     * <p>
     * دو کارِ اضافه نسبت به واردکردنِ ساده، به همین ترتیب:
     * <ol>
     *   <li><b>حلقه‌زدایی</b> — هر ریدایرکتی که {@code fromPath}ش برابرِ مسیرِ
     *       <i>تازه</i> است حذف می‌شود. بدونِ این، برگرداندنِ اسلاگ به مقدارِ قبلی
     *       یک ریدایرکتِ خودارجاع می‌ساخت و صفحه هرگز باز نمی‌شد.</li>
     *   <li><b>صاف‌کردن</b> — هر ریدایرکتی که مقصدش مسیرِ <i>قدیم</i> بود، مستقیم به
     *       مسیرِ تازه می‌رود.</li>
     * </ol>
     * ترتیب عمدی است: اول حلقه‌زدایی، بعد ثبت، بعد صاف‌کردن. اگر صاف‌کردن زودتر
     * انجام می‌شد، رکوردی که همین الان حذف شده بود دوباره به‌روز می‌شد.
     */
    public Map<String, Object> addOneFlattened(String fromRaw, String toRaw, String note,
                                               java.util.function.Predicate<String> targetExists) {
        String from = normalize(fromRaw);
        String to = toRaw == null ? "" : toRaw.trim();
        String toNormalized = normalize(to);

        // مسیرِ قدیم و تازه یکی‌اند — چیزی برایِ ثبت نیست و ثبتش یعنی حلقه
        if (from.equals(toNormalized)) {
            Map<String, Object> same = new LinkedHashMap<>();
            same.put("created", 0);
            same.put("updated", 0);
            same.put("skippedMissingTarget", List.of());
            same.put("skippedInvalid", List.of(fromRaw + " → " + toRaw));
            same.put("unchanged", true);
            same.put("total", repo.count());
            return same;
        }

        // ۱) حلقه‌زدایی
        int loopsRemoved = 0;
        Optional<LegacyRedirect> selfTarget = repo.findByFromPath(toNormalized);
        if (selfTarget.isPresent()) {
            repo.delete(selfTarget.get());
            loopsRemoved++;
        }

        // ۲) ثبتِ خودِ ریدایرکت — با همان اعتبارسنجیِ مقصد که واردکردنِ دسته‌ای دارد
        Map<String, Object> result = importRows(List.of(new MappingRow(from, to, note)), targetExists);

        // اگر ثبت نشد (مقصد وجود ندارد یا مسیر رزرو است) زنجیره را هم دست نمی‌زنیم:
        // صاف‌کردن به مقصدی که پذیرفته نشده، خرابیِ بزرگ‌تری است از زنجیرهٔ دوپرشی.
        int flattened = 0;
        if ((int) result.get("created") + (int) result.get("updated") > 0) {
            for (LegacyRedirect r : repo.findAll()) {
                if (r.getToPath() == null) continue;
                if (!normalize(r.getToPath()).equals(from)) continue;
                if (normalize(r.getFromPath()).equals(toNormalized)) continue;   // حلقه نساز
                r.setToPath(to);
                repo.save(r);
                flattened++;
            }
        }

        result = new LinkedHashMap<>(result);
        result.put("flattened", flattened);
        result.put("loopsRemoved", loopsRemoved);
        if (flattened > 0 || loopsRemoved > 0) reloadKeys();
        log.info("ریدایرکتِ اسلاگ: {} → {} · {} زنجیره صاف شد · {} حلقه حذف شد.",
                from, to, flattened, loopsRemoved);
        return result;
    }

    /**
     * واردکردنِ دسته‌ای.
     * <p>
     * 🔴 مقصد <b>پیش از</b> ذخیره باید وجود داشته باشد. ریدایرکت به صفحه‌ای که ۴۰۴
     * می‌دهد از خودِ ۴۰۴ بدتر است: گوگل دو درخواست می‌دهد و همان نتیجه را می‌گیرد،
     * و ما هم به‌اشتباه فکر می‌کنیم مهاجرت انجام شده.
     *
     * @param targetExists چکِ وجودِ مقصد — از بیرون داده می‌شود تا این سرویس به
     *                     مقاله و محصول گره نخورد.
     */
    public Map<String, Object> importRows(List<MappingRow> rows,
                                          java.util.function.Predicate<String> targetExists) {
        List<String> skippedMissingTarget = new ArrayList<>();
        List<String> skippedInvalid = new ArrayList<>();
        int created = 0, updated = 0;

        for (MappingRow row : rows) {
            String from = normalize(row.fromPath());
            String to = row.toPath() == null ? "" : row.toPath().trim();

            if (to.isEmpty() || !to.startsWith("/") || isReservedPath(from)) {
                skippedInvalid.add(row.fromPath() + " → " + row.toPath());
                continue;
            }
            if (!targetExists.test(to)) {
                skippedMissingTarget.add(row.fromPath() + " → " + to);
                continue;
            }

            Optional<LegacyRedirect> existing = repo.findByFromPath(from);
            LegacyRedirect r = existing.orElseGet(LegacyRedirect::new);
            r.setFromPath(from);
            r.setToPath(to);
            r.setNote(row.note());
            repo.save(r);
            if (existing.isPresent()) updated++; else created++;
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("created", created);
        out.put("updated", updated);
        out.put("skippedMissingTarget", skippedMissingTarget);
        out.put("skippedInvalid", skippedInvalid);
        out.put("total", repo.count());
        reloadKeys();
        log.info("واردکردنِ ریدایرکتِ قدیمی: {} تازه، {} به‌روز، {} مقصدِ گم‌شده، {} ردیفِ نامعتبر.",
                created, updated, skippedMissingTarget.size(), skippedInvalid.size());
        return out;
    }
}
