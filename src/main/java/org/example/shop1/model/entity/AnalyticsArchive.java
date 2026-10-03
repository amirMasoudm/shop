package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * سندِ یک آرشیوِ گرفته‌شده از دادهٔ خام.
 * <p>
 * 🔴 وجودِ این سند <b>شرطِ حذف</b> است: جابِ نگه‌داری اول آرشیو می‌نویسد و این سند را
 * ثبت می‌کند، و فقط بعد از موفقیتِ کامل رویدادها را حذف می‌کند. اگر آرشیو شکست بخورد
 * هیچ چیزی حذف نمی‌شود. این ترتیب «اجباری‌بودنِ آرشیو» را ساختاری می‌کند و به
 * یادآوریِ هیچ‌کس وابسته‌اش نمی‌کند.
 */
@Document(collection = "analytics_archives")
public class AnalyticsArchive {

    @Id
    private String id;

    private Instant from;
    private Instant to;

    /** نامِ فایل در پوشهٔ آرشیو — نه مسیرِ کامل، تا جابه‌جاییِ پوشه سند را نشکند. */
    private String file;

    private long rowCount;

    /** واقعاً محاسبه می‌شود؛ آرشیوِ خرابِ بی‌خبر بدتر از نبودِ آرشیو است. */
    private String sha256;

    private long sizeBytes;

    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Instant getFrom() { return from; }
    public void setFrom(Instant from) { this.from = from; }

    public Instant getTo() { return to; }
    public void setTo(Instant to) { this.to = to; }

    public String getFile() { return file; }
    public void setFile(String file) { this.file = file; }

    public long getRowCount() { return rowCount; }
    public void setRowCount(long rowCount) { this.rowCount = rowCount; }

    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
