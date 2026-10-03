package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * یک آدرسِ قدیمیِ ایندکس‌شده و مقصدِ تازه‌اش.
 * <p>
 * 🔴 <b>چرا جدا از {@code product_redirects}:</b> آن یکی کلیدش <b>اسلاگِ محصول</b> است و
 * فقط وقتی کار می‌کند که آدرس زیرِ {@code /shop/product/} باشد. آدرس‌های وردپرسِ قدیم
 * روی <b>ریشهٔ دامنه</b> نشسته‌اند ({@code /عیبیابی-میکروتیک-.../})، پس کلید باید
 * مسیرِ کامل باشد، نه اسلاگ.
 * <p>
 * ⚠️ این تنها بخشِ مهاجرت است که برگشت‌ناپذیر است: سرور را می‌شود برگرداند، ولی وقتی
 * گوگل صفحه‌ای را ۴۰۴ ببیند و از ایندکس بیندازد، اعتبارِ چندساله برمی‌گردد به صفر.
 */
@Document(collection = "legacy_redirects")
public class LegacyRedirect {

    @Id
    private String id;

    /**
     * مسیرِ قدیمی، <b>همیشه نرمال‌شده</b>. هم موقعِ ذخیره و هم موقعِ جست‌وجو از
     * {@code LegacyRedirectService.normalize} رد می‌شود، وگرنه بخشِ بزرگی از
     * آدرس‌ها در عمل پیدا نمی‌شوند.
     */
    private String fromPath;

    /** مقصدِ داخلی — همیشه با «/» شروع می‌شود. */
    private String toPath;

    /**
     * چند بار خورده.
     * <p>
     * دلیلِ وجودش تصمیم‌گیری است نه کنجکاوی: از ۲۷۹ آدرسِ نگاشت‌شده عمداً فقط
     * پُرارزش‌ها وارد شدند، و بعد از سوییچ همین ستون می‌گوید کدام‌یک از بقیه واقعاً
     * ترافیک دارد و ارزشِ اضافه‌شدن. این از حدس‌زدن ارزان‌تر است.
     */
    private long hits;

    private Instant lastHitAt;

    private String note;

    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFromPath() { return fromPath; }
    public void setFromPath(String fromPath) { this.fromPath = fromPath; }

    public String getToPath() { return toPath; }
    public void setToPath(String toPath) { this.toPath = toPath; }

    public long getHits() { return hits; }
    public void setHits(long hits) { this.hits = hits; }

    public Instant getLastHitAt() { return lastHitAt; }
    public void setLastHitAt(Instant lastHitAt) { this.lastHitAt = lastHitAt; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
