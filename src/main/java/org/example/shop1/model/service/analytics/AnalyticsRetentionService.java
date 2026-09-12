package org.example.shop1.model.service.analytics;

import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.example.shop1.config.AnalyticsProperties;
import org.example.shop1.model.entity.AnalyticsArchive;
import org.example.shop1.model.reposritory.AnalyticsArchiveRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * نگه‌داریِ دادهٔ خام: <b>اول آرشیو، بعد حذف</b> — و نه جز این.
 * <p>
 * 🔴 <b>ترتیبِ این چهار قدم قابلِ مذاکره نیست:</b> پیداکردنِ بازهٔ کهنه، نوشتنِ
 * آرشیوِ کامل، ثبتِ سندِ آرشیو، و تنها بعد از موفقیتِ کامل، حذف. اگر هر جای این
 * زنجیره بشکند — دیسکِ پر، پوشهٔ فقط-خواندنی، خطای نوشتن — <b>هیچ رویدادی حذف
 * نمی‌شود</b> و هشدار در پنل بالا می‌آید.
 * <p>
 * چرا ایندکسِ TTLِ مونگو این کار را نمی‌کند: TTL بی‌صدا و بی‌قید حذف می‌کند و
 * نمی‌شود شرطِ «فقط اگر آرشیو گرفته شده» را به آن داد. خواستهٔ مالک دقیقاً همان
 * شرط است، پس حذف باید کارِ خودمان باشد.
 * <p>
 * هشدارِ پنل عمداً <b>مشتق</b> است نه ذخیره‌شده: «رویدادِ کهنه‌تر از مدتِ نگه‌داری
 * هنوز وجود دارد» خودش یعنی آرشیو عقب افتاده. این‌طور هشدار با ری‌استارت گم نمی‌شود
 * و بعد از موفقیتِ آرشیو هم خودبه‌خود پاک می‌شود.
 */
@Service
public class AnalyticsRetentionService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsRetentionService.class);

    private static final String COLLECTION = "user_events";
    private static final int CURSOR_BATCH = 1000;

    private final MongoTemplate mongo;
    private final AnalyticsArchiveRepository archiveRepo;
    private final AnalyticsProperties props;

    /** آخرین خطای آرشیو — فقط برای نمایشِ جزئیات؛ خودِ «عقب‌افتادگی» مشتق می‌شود. */
    private volatile String lastError;
    private volatile Instant lastRunAt;

    public AnalyticsRetentionService(MongoTemplate mongo, AnalyticsArchiveRepository archiveRepo,
                                     AnalyticsProperties props) {
        this.mongo = mongo;
        this.archiveRepo = archiveRepo;
        this.props = props;
    }

    public Instant cutoff() {
        return Instant.now().minus(props.getRetentionDays(), ChronoUnit.DAYS);
    }

    /**
     * اجرای چرخهٔ آرشیو-سپس-حذف.
     *
     * @return تعدادِ رویدادِ آرشیو و حذف‌شده؛ صفر یعنی چیزی برای آرشیو نبود.
     */
    public long runRetention() {
        lastRunAt = Instant.now();
        Instant cutoff = cutoff();
        Document filter = new Document("at", new Document("$lt", cutoff));

        long count = mongo.getCollection(COLLECTION).countDocuments(filter);
        if (count == 0) {
            lastError = null;
            return 0;
        }

        Instant from = oldestAt();
        String fileName = "events-" + from.toString().replace(':', '-')
                + "--" + cutoff.toString().replace(':', '-') + ".ndjson.gz";

        try {
            Path dir = Paths.get(props.getArchiveDir());
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);

            ArchiveResult written = writeArchive(filter, target);

            // سندِ آرشیو پیش از حذف ثبت می‌شود: اگر همین ثبت بشکند، حذف اصلاً اتفاق نمی‌افتد.
            AnalyticsArchive archive = new AnalyticsArchive();
            archive.setFrom(from);
            archive.setTo(cutoff);
            archive.setFile(fileName);
            archive.setRowCount(written.rows());
            archive.setSha256(written.sha256());
            archive.setSizeBytes(Files.size(target));
            archiveRepo.save(archive);

            // ✅ و فقط حالا
            long deleted = mongo.getCollection(COLLECTION).deleteMany(filter).getDeletedCount();
            lastError = null;
            log.info("آرشیو کامل شد: {} ردیف در {} ({} بایت)، سپس {} رویداد حذف شد.",
                    written.rows(), fileName, archive.getSizeBytes(), deleted);
            warnIfArchiveDirLarge();
            return deleted;

        } catch (Exception e) {
            // 🔴 هیچ حذفی انجام نمی‌شود. این تنها رفتارِ درست است: دادهٔ بی‌آرشیوِ
            // حذف‌شده برنمی‌گردد، ولی دادهٔ حذف‌نشده فقط جا می‌گیرد.
            lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.error("آرشیو شکست خورد؛ هیچ رویدادی حذف نشد. {} رویداد از قبل از {} معطل ماند.",
                    count, cutoff, e);
            return 0;
        }
    }

    private record ArchiveResult(long rows, String sha256) {}

    /**
     * نوشتنِ NDJSONِ فشرده با کرسر — کلِ بازه هرگز در حافظه جمع نمی‌شود.
     * سرور ۳٫۸ گیگ رم دارد و بازهٔ ۹۰ روزه می‌تواند ده‌ها هزار سند باشد.
     */
    private ArchiveResult writeArchive(Document filter, Path target) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        JsonWriterSettings json = JsonWriterSettings.builder().build();
        long rows = 0;

        try (OutputStream fileOut = Files.newOutputStream(target);
             DigestOutputStream digestOut = new DigestOutputStream(fileOut, digest);
             GZIPOutputStream gzip = new GZIPOutputStream(digestOut);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzip, StandardCharsets.UTF_8));
             MongoCursor<Document> cursor = mongo.getCollection(COLLECTION)
                     .find(filter).batchSize(CURSOR_BATCH).iterator()) {

            while (cursor.hasNext()) {
                writer.write(cursor.next().toJson(json));
                writer.write('\n');
                rows++;
            }
        }

        // هشِ فایلِ نهاییِ روی دیسک، نه بافرِ میانی — آرشیوِ خرابِ بی‌خبر بدتر از نبودِ آرشیو است.
        return new ArchiveResult(rows, sha256Of(target));
    }

    public String sha256Of(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (OutputStream sink = OutputStream.nullOutputStream();
             DigestOutputStream out = new DigestOutputStream(sink, digest)) {
            Files.copy(file, out);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : digest.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private Instant oldestAt() {
        Document first = mongo.getCollection(COLLECTION)
                .find().sort(new Document("at", 1)).limit(1).first();
        if (first == null) return Instant.now();
        Object at = first.get("at");
        return at instanceof java.util.Date d ? d.toInstant() : Instant.now();
    }

    private void warnIfArchiveDirLarge() {
        try {
            Path dir = Paths.get(props.getArchiveDir());
            if (!Files.isDirectory(dir)) return;
            long total;
            try (var stream = Files.list(dir)) {
                total = stream.filter(Files::isRegularFile).mapToLong(p -> {
                    try { return Files.size(p); } catch (Exception e) { return 0; }
                }).sum();
            }
            long mb = total / (1024 * 1024);
            if (mb >= props.getArchiveWarnMb()) {
                // هشدار، نه حذف: هیچ آرشیوی خودکار پاک نمی‌شود.
                log.warn("پوشهٔ آرشیو به {} مگابایت رسید (آستانه {}). هیچ آرشیوی خودکار حذف نمی‌شود؛ تصمیمش با مالک است.",
                        mb, props.getArchiveWarnMb());
            }
        } catch (Exception e) {
            log.debug("اندازه‌گیریِ پوشهٔ آرشیو ناموفق بود: {}", e.toString());
        }
    }

    /** وضعیت برای نمای «داده و آرشیو» در پنل. */
    public Map<String, Object> status() {
        Instant cutoff = cutoff();
        long pending = mongo.getCollection(COLLECTION)
                .countDocuments(new Document("at", new Document("$lt", cutoff)));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("retentionDays", props.getRetentionDays());
        out.put("cutoff", cutoff);
        out.put("pendingRows", pending);
        // هشدار مشتق است: وجودِ رویدادِ کهنه یعنی آرشیو عقب افتاده، فارغ از اینکه
        // چرا — پس با ری‌استارتِ اپ گم نمی‌شود و بعد از موفقیت خودبه‌خود می‌خوابد.
        out.put("archiveOverdue", pending > 0);
        out.put("lastError", lastError);
        out.put("lastRunAt", lastRunAt);
        out.put("archiveDir", props.getArchiveDir());
        return out;
    }

    public Path archiveFile(String fileName) {
        Path base = Paths.get(props.getArchiveDir()).toAbsolutePath().normalize();
        Path resolved = base.resolve(fileName).normalize();
        // نامِ فایل از دیتابیس می‌آید، ولی چکِ traversal می‌ماند: اگر روزی سند از
        // مسیرِ دیگری پر شد، نتواند به بیرونِ پوشهٔ آرشیو اشاره کند.
        if (!resolved.startsWith(base)) {
            throw new IllegalArgumentException("مسیرِ آرشیو نامعتبر است");
        }
        return resolved;
    }
}
