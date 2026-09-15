package org.example.shop1.controller;

import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.ActivityLog;
import org.example.shop1.model.entity.AnalyticsArchive;
import org.example.shop1.model.entity.Campaign;
import org.example.shop1.model.entity.DailyStats;
import org.example.shop1.model.reposritory.AnalyticsArchiveRepository;
import org.example.shop1.model.reposritory.CampaignRepository;
import org.example.shop1.model.reposritory.DailyStatsRepository;
import org.example.shop1.model.reposritory.VisitorRepository;
import org.example.shop1.model.service.ActivityLogService;
import org.example.shop1.model.service.analytics.AnalyticsRetentionService;
import org.example.shop1.model.service.analytics.DailyStatsService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * اندپوینت‌های تبِ «رفتارِ کاربران».
 * <p>
 * 🔴 کلِ این مسیر در {@code SecurityConfig} فقط {@code ADMIN} است. «سفرِ کاربر» و
 * خروجی‌گرفتن دادهٔ شخصی‌اند و برخلافِ تبِ پشتیبانیِ چت — که عمداً برای همهٔ نقش‌ها
 * باز بود — این یکی نباید در اختیارِ نقش‌های فروش باشد.
 */
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsAdminController {

    private static final int JOURNEY_PAGE = 200;
    private static final int VISITORS_PAGE = 100;

    private final MongoTemplate mongo;
    private final DailyStatsRepository dailyStatsRepo;
    private final DailyStatsService dailyStatsService;
    private final AnalyticsArchiveRepository archiveRepo;
    private final CampaignRepository campaignRepo;
    private final AnalyticsRetentionService retention;
    private final VisitorRepository visitorRepo;
    private final ActivityLogService activityLog;

    public AnalyticsAdminController(MongoTemplate mongo, DailyStatsRepository dailyStatsRepo,
                                    DailyStatsService dailyStatsService,
                                    AnalyticsArchiveRepository archiveRepo,
                                    CampaignRepository campaignRepo,
                                    AnalyticsRetentionService retention,
                                    VisitorRepository visitorRepo,
                                    ActivityLogService activityLog) {
        this.mongo = mongo;
        this.dailyStatsRepo = dailyStatsRepo;
        this.dailyStatsService = dailyStatsService;
        this.archiveRepo = archiveRepo;
        this.campaignRepo = campaignRepo;
        this.retention = retention;
        this.visitorRepo = visitorRepo;
        this.activityLog = activityLog;
    }

    // ==========================================================
    // نماهای ۱، ۲، ۵ — فقط از daily_stats
    // ==========================================================

    /**
     * 🔴 عمداً فقط {@code daily_stats} را می‌خواند و هرگز {@code user_events} را.
     * اگر داشبورد روی دادهٔ خام کوئری بزند، ماهِ ششم باز نمی‌شود.
     */
    @GetMapping("/overview")
    public ResponseEntity<List<DailyStats>> overview(@RequestParam String from, @RequestParam String to) {
        return ResponseEntity.ok(dailyStatsRepo.findRange(from, to, Sort.by(Sort.Direction.ASC, "_id")));
    }

    /** ساختِ دستیِ جمع‌بندیِ یک روز — برایِ بازسازی و برای اجرای فوری بعد از تغییر. */
    @PostMapping("/rollup")
    public ResponseEntity<DailyStats> rollup(@RequestParam String date) {
        return ResponseEntity.ok(dailyStatsService.rollup(LocalDate.parse(date)));
    }

    /**
     * گزارشِ کارزار — کلیک ← بازدید ← محصول ← سبد ← سفارش، به‌ازای هر کارزار.
     * <p>
     * 🔴 مثلِ بقیهٔ نماهای تجمیعی، <b>فقط</b> از {@code daily_stats} می‌خواند و هرگز از
     * {@code user_events}. تنها اتصالش به دفترِ {@code campaigns} است که چند ده سند
     * بیشتر نیست و فقط نامِ نمایشی و هزینه را می‌آورد.
     * <p>
     * ستونِ «هزینه به‌ازای هر سفارش» همان چیزی است که این گزارش را از «جالب» به
     * «قابلِ تصمیم‌گیری» می‌برد — و دقیقاً به همین دلیل وقتی هزینه وارد نشده، به‌جای
     * صفرِ گمراه‌کننده {@code null} برمی‌گردد.
     */
    @GetMapping("/campaigns")
    public ResponseEntity<List<Map<String, Object>>> campaignReport(
            @RequestParam String from, @RequestParam String to) {

        Map<String, long[]> totals = new LinkedHashMap<>();
        for (DailyStats day : dailyStatsRepo.findRange(from, to, Sort.by(Sort.Direction.ASC, "_id"))) {
            if (day.getByCampaign() == null) continue;
            for (Map.Entry<String, DailyStats.CampaignStats> e : day.getByCampaign().entrySet()) {
                DailyStats.CampaignStats c = e.getValue();
                long[] acc = totals.computeIfAbsent(e.getKey(), k -> new long[5]);
                acc[0] += c.getClicks();
                acc[1] += c.getVisits();
                acc[2] += c.getProductViews();
                acc[3] += c.getAddToCart();
                acc[4] += c.getOrders();
            }
        }

        Map<String, Campaign> registry = new LinkedHashMap<>();
        for (Campaign c : campaignRepo.findAll()) registry.put(c.getSlug(), c);

        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, long[]> e : totals.entrySet()) {
            long[] a = e.getValue();
            Campaign c = registry.get(e.getKey());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("slug", e.getKey());
            // کارزاری که در دفتر نیست هم نمایش داده می‌شود: یعنی کسی لینک را دستی
            // برچسب زده. پنهان‌کردنش، ترافیکی را نامرئی می‌کرد که واقعاً وجود دارد.
            row.put("name", c != null ? c.getName() : e.getKey());
            row.put("registered", c != null);
            row.put("source", c != null ? c.getSource() : null);
            row.put("medium", c != null ? c.getMedium() : null);
            row.put("active", c != null && c.isActive());
            row.put("clicks", a[0]);
            row.put("visits", a[1]);
            row.put("productViews", a[2]);
            row.put("addToCart", a[3]);
            row.put("orders", a[4]);
            Long cost = c != null ? c.getCost() : null;
            row.put("cost", cost);
            row.put("costPerOrder", cost != null && a[4] > 0 ? cost / a[4] : null);
            out.add(row);
        }
        out.sort((x, y) -> Long.compare((Long) y.get("visits"), (Long) x.get("visits")));
        return ResponseEntity.ok(out);
    }

    // ==========================================================
    // نمای ۳ — بازدیدکنندگان
    // ==========================================================

    @GetMapping("/visitors")
    public ResponseEntity<List<Map<String, Object>>> visitors(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "0") int page) {

        Document match = new Document();
        // فیلترهای آماده: هر کدام «چه کسی این کار را کرد ولی آن یکی را نه»
        String needType = switch (filter == null ? "" : filter) {
            case "cartNoOrder" -> "ADD_TO_CART";
            case "rfq" -> "RFQ_SUBMIT";
            case "stockNotify" -> "STOCK_NOTIFY_SUBSCRIBE";
            default -> null;
        };

        List<String> ids = null;
        if (needType != null) {
            ids = mongo.getCollection("user_events").distinct("anonId",
                    new Document("type", needType), String.class).into(new ArrayList<>());
            if ("cartNoOrder".equals(filter)) {
                List<String> ordered = mongo.getCollection("user_events").distinct("anonId",
                        new Document("type", "ORDER_PLACED"), String.class).into(new ArrayList<>());
                ids.removeAll(ordered);
            }
            if (ids.isEmpty()) return ResponseEntity.ok(List.of());
            match.append("_id", new Document("$in", ids));
        }

        List<Document> docs = mongo.getCollection("visitors")
                .find(match).sort(new Document("lastSeenAt", -1))
                .skip(page * VISITORS_PAGE).limit(VISITORS_PAGE)
                .into(new ArrayList<>());

        List<Map<String, Object>> out = new ArrayList<>();
        for (Document d : docs) {
            Map<String, Object> row = new LinkedHashMap<>(d);
            String anonId = String.valueOf(d.get("_id"));

            long liveVisits = mongo.getCollection("user_events").distinct("sessionId",
                    new Document("anonId", anonId), String.class).into(new ArrayList<>()).size();
            long liveProductViews = mongo.getCollection("user_events")
                    .countDocuments(new Document("anonId", anonId).append("type", "PRODUCT_VIEW"));
            long liveOrders = mongo.getCollection("user_events")
                    .countDocuments(new Document("anonId", anonId).append("type", "ORDER_PLACED"));

            // 🔴 شمارشِ زنده به‌تنهایی بعد از اولین آرشیو صفر می‌شد و کلِ ستون «انگار
            // هیچ‌کس هیچ‌وقت نیامده» را نشان می‌داد. سه ستونِ اصلی حالا جمعِ زنده و
            // آرشیوند، و سهمِ آرشیو جدا هم برمی‌گردد تا پنل بتواند بگوید از کجا آمده.
            Document archived = (Document) row.remove("archived");
            row.put("visits", liveVisits + num(archived, "visits"));
            row.put("productViews", liveProductViews + num(archived, "productViews"));
            row.put("orders", liveOrders + num(archived, "orders"));
            row.put("archivedVisits", num(archived, "visits"));
            row.put("archivedProductViews", num(archived, "productViews"));
            row.put("archivedOrders", num(archived, "orders"));
            row.put("archivedFrom", isoDate(archived, "from"));
            row.put("archivedTo", isoDate(archived, "to"));
            out.add(row);
        }
        return ResponseEntity.ok(out);
    }

    private static long num(Document d, String key) {
        if (d == null) return 0;
        return d.get(key) instanceof Number n ? n.longValue() : 0;
    }

    /** تاریخ به ISO-8601 برمی‌گردد؛ تبدیل به شمسی کارِ خودِ پنل است. */
    private static String isoDate(Document d, String key) {
        if (d == null) return null;
        return d.get(key) instanceof java.util.Date date ? date.toInstant().toString() : null;
    }

    // ==========================================================
    // نمای ۴ — سفرِ کاربر (تنها نمایی که اجازهٔ دادهٔ خام دارد)
    // ==========================================================

    /**
     * خطِ زمانیِ یک نفر. {@code visitors.userId} همهٔ {@code anonId}هایش را می‌دهد —
     * یعنی گوشی و لپ‌تاپش هر دو زیرِ یک سفر می‌آیند.
     */
    @GetMapping("/journey")
    public ResponseEntity<Map<String, Object>> journey(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String anonId,
            @RequestParam(defaultValue = "0") int page) {

        List<String> anonIds = new ArrayList<>();
        if (userId != null && !userId.isBlank()) {
            visitorRepo.findByUserId(userId).forEach(v -> anonIds.add(v.getId()));
        } else if (anonId != null && !anonId.isBlank()) {
            anonIds.add(anonId);
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "شناسهٔ کاربر یا شناسهٔ ناشناس لازم است");
        }
        if (anonIds.isEmpty()) {
            return ResponseEntity.ok(Map.of("anonIds", List.of(), "events", List.of()));
        }

        List<Document> events = mongo.getCollection("user_events")
                .find(new Document("anonId", new Document("$in", anonIds)))
                .sort(new Document("at", -1))
                .skip(page * JOURNEY_PAGE).limit(JOURNEY_PAGE)
                .into(new ArrayList<>());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("anonIds", anonIds);
        out.put("events", events);
        return ResponseEntity.ok(out);
    }

    // ==========================================================
    // نمای ۶ — داده و آرشیو
    // ==========================================================

    /**
     * @param days آستانهٔ دلخواه فقط برای <b>پیش‌نمایش</b>: پنل پیش از تأیید با همین
     *             می‌پرسد «چند رویداد آرشیو و حذف می‌شود؟». آستانهٔ جابِ شبانه عوض نمی‌شود.
     */
    @GetMapping("/retention-status")
    public ResponseEntity<Map<String, Object>> retentionStatus(
            @RequestParam(required = false) Integer days) {
        return ResponseEntity.ok(retention.status(days));
    }

    @GetMapping("/archives")
    public ResponseEntity<List<AnalyticsArchive>> archives() {
        return ResponseEntity.ok(archiveRepo.findAllByOrderByCreatedAtDesc());
    }

    /**
     * اجرای دستیِ چرخهٔ آرشیو — برایِ وقتی مالک نمی‌خواهد تا شب صبر کند.
     * <p>
     * {@code days} آستانه را <b>فقط برای همین یک اجرا</b> عوض می‌کند. دلیلِ وجودش
     * تجربهٔ میدانی بود: برای دیدنِ یک چرخهٔ کامل باید {@code retention-days} را در
     * فایل صفر می‌کردیم، کلِ jar را از نو می‌ساختیم و ایمیج را دوباره بیلد می‌کردیم —
     * و اگر یادمان می‌رفت برش گردانیم، هر شب کلِ دادهٔ خام آرشیو و حذف می‌شد.
     * <p>
     * حذف همچنان فقط بعد از آرشیوِ موفق انجام می‌شود، پس دادهٔ حذف‌شده در فایلِ
     * قابل‌دانلود هست. با این حال چون دستور از آدم می‌آید و دادهٔ خام را برمی‌دارد،
     * در لاگِ فعالیت ثبت می‌شود.
     */
    @PostMapping("/archive-now")
    public ResponseEntity<Map<String, Object>> archiveNow(
            @RequestParam(required = false) Integer days) {
        long deleted = days == null ? retention.runRetention() : retention.runRetention(days);
        Map<String, Object> out = new LinkedHashMap<>(retention.status());
        out.put("deleted", deleted);
        if (deleted > 0) {
            activityLog.record(ActivityLog.Action.ANALYTICS_ERASE, ActivityLog.Source.MANUAL,
                    ActivityLogService.ENTITY_ANALYTICS, null, "اجرای دستیِ آرشیو",
                    "archive", "آستانه " + (days == null ? out.get("retentionDays") : days) + " روز",
                    deleted + " رویداد آرشیو و حذف شد");
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping("/archives/{id}/download")
    public ResponseEntity<InputStreamResource> downloadArchive(@PathVariable String id) throws Exception {
        AnalyticsArchive archive = archiveRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "آرشیو پیدا نشد"));
        Path file = retention.archiveFile(archive.getFile());
        if (!Files.exists(file)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "فایلِ آرشیو روی دیسک نیست");
        }
        logExport("archive:" + archive.getFile(), archive.getRowCount());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + archive.getFile() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, private")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(Files.newInputStream(file)));
    }

    // ==========================================================
    // خروجیِ درخواستی
    // ==========================================================

    /**
     * 🔴 استریم‌شده با کرسر، نه ساختنِ فایل در حافظه. سرور ۳٫۸ گیگ رم دارد و بازهٔ
     * ۹۰روزه ده‌ها هزار سند است؛ جمع‌کردنش در حافظه یعنی از کار افتادنِ اپ.
     */
    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> export(
            @RequestParam String from, @RequestParam String to,
            @RequestParam(required = false) String type,
            jakarta.servlet.http.HttpServletRequest request) {

        // ستونِ url باید واقعاً قابلِ کلیک باشد، پس از همان میزبانی ساخته می‌شود که
        // ادمین با آن وارد پنل شده — نه از یک مقدارِ هاردکد.
        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443
                        ? "" : ":" + request.getServerPort());

        Instant fromAt = Instant.parse(from);
        Instant toAt = Instant.parse(to);
        Document filter = new Document("at", new Document("$gte", fromAt).append("$lt", toAt));
        if (type != null && !type.isBlank()) filter.append("type", type);

        long rows = mongo.getCollection("user_events").countDocuments(filter);
        logExport(from + ".." + to, rows);

        StreamingResponseBody body = out -> {
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
                 MongoCursor<Document> cursor = mongo.getCollection("user_events")
                         .find(filter).batchSize(1000).iterator()) {

                w.write("﻿");   // BOM تا اکسل فارسی را درست باز کند
                // path خام می‌ماند (ماشین‌خوان و دقیق) و دو ستونِ تازه کنارش می‌آید:
                // pathDecoded برایِ خواندنِ آدم، و url برایِ کلیک‌کردن. خواستهٔ صریح
                // این بود که «هم برای ماشین خوانا باشد هم برای آدم».
                w.write("at,type,anonId,userId,sessionId,channel,campaign,path,pathDecoded,url,entityType,entityId,entityName,device,city\n");
                while (cursor.hasNext()) {
                    Document d = cursor.next();
                    w.write(csv(d.get("at")));   w.write(',');
                    w.write(csv(d.get("type"))); w.write(',');
                    w.write(csv(d.get("anonId"))); w.write(',');
                    w.write(csv(d.get("userId"))); w.write(',');
                    w.write(csv(d.get("sessionId"))); w.write(',');
                    w.write(csv(d.get("channel"))); w.write(',');
                    w.write(csv(d.get("campaign"))); w.write(',');
                    Object rawPath = d.get("path");
                    w.write(csv(rawPath)); w.write(',');
                    w.write(csv(decodePath(rawPath))); w.write(',');
                    w.write(csv(rawPath == null ? null : baseUrl + rawPath)); w.write(',');
                    w.write(csv(d.get("entityType"))); w.write(',');
                    w.write(csv(d.get("entityId"))); w.write(',');
                    w.write(csv(d.get("entityName"))); w.write(',');
                    w.write(csv(d.get("device"))); w.write(',');
                    w.write(csv(d.get("city")));
                    w.write('\n');
                }
                w.flush();
            }
        };

        String fileName = "user-events-" + from.substring(0, 10) + "--" + to.substring(0, 10) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, private")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    // ==========================================================
    // حذفِ دادهٔ یک کاربر
    // ==========================================================

    /**
     * پاک‌کردنِ ردِ یک کاربر — رویدادها و سندهای {@code visitors}ِ همهٔ دستگاه‌هایش.
     * <p>
     * {@code daily_stats} عمداً دست نمی‌خورد: آمارِ تجمیعی هویت ندارد و بازسازی‌اش
     * هم بی‌معنی است و هم نمودارِ تاریخی را خراب می‌کند.
     */
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> eraseUser(@PathVariable String userId) {
        List<String> anonIds = new ArrayList<>();
        visitorRepo.findByUserId(userId).forEach(v -> anonIds.add(v.getId()));

        Document filter = anonIds.isEmpty()
                ? new Document("userId", userId)
                : new Document("$or", List.of(
                        new Document("userId", userId),
                        new Document("anonId", new Document("$in", anonIds))));

        long events = mongo.getCollection("user_events").deleteMany(filter).getDeletedCount();
        long visitors = anonIds.isEmpty() ? 0
                : mongo.getCollection("visitors")
                        .deleteMany(new Document("_id", new Document("$in", anonIds))).getDeletedCount();

        activityLog.record(ActivityLog.Action.ANALYTICS_ERASE, ActivityLog.Source.MANUAL,
                ActivityLogService.ENTITY_ANALYTICS, userId, "حذفِ دادهٔ رفتاریِ کاربر",
                "erase", events + " رویداد", visitors + " بازدیدکننده");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("deletedEvents", events);
        out.put("deletedVisitors", visitors);
        return ResponseEntity.ok(out);
    }

    /**
     * 🔴 گرفتنِ خروجی خودش یک کنشِ کارمندی روی دادهٔ شخصی است و در
     * {@code activity_logs} ثبت می‌شود — همان لاگی که عمداً از {@code user_events}
     * جدا نگه داشته شد.
     */
    private void logExport(String range, long rows) {
        activityLog.record(ActivityLog.Action.ANALYTICS_EXPORT, ActivityLog.Source.MANUAL,
                ActivityLogService.ENTITY_ANALYTICS, null, "خروجیِ دادهٔ رفتاری",
                "export", range, rows + " ردیف");
    }

    /**
     * مسیرِ درصد-کدشده را خوانا می‌کند.
     * <p>
     * ⚠️ داخلِ {@code try/catch}: مسیرِ ناقص یا خرابِ درصد-کدشده نباید کلِ خروجی را
     * بشکند — اصل همیشه در ستونِ {@code path} دست‌نخورده هست.
     */
    static String decodePath(Object rawPath) {
        if (rawPath == null) return null;
        String s = String.valueOf(rawPath);
        try {
            return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private String csv(Object value) {
        if (value == null) return "";
        // درایورِ مونگو تاریخ را java.util.Date می‌دهد و toString آن وابسته به
        // منطقهٔ زمانی و زبانِ سرور است («Sat Sep 12 … IRST 2026»). خروجی قرار است
        // در اکسل و ابزارِ تحلیل باز شود، پس ISO-8601 تنها قالبِ قابلِ اتکاست.
        if (value instanceof java.util.Date d) return d.toInstant().toString();
        String s = String.valueOf(value);
        if (s.indexOf(',') < 0 && s.indexOf('"') < 0 && s.indexOf('\n') < 0) return s;
        return '"' + s.replace("\"", "\"\"") + '"';
    }
}
