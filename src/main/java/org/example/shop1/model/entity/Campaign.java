package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * یک کارزارِ بازاریابی — دفترِ مرکزیِ برچسب‌ها.
 * <p>
 * 🔴 <b>چرا اصلاً دفتر لازم است:</b> لینکی که دستی و بی‌ثبت ساخته شود، برچسبش هر بار
 * کمی فرق می‌کند و در گزارش به چند کارزارِ جدا تبدیل می‌شود. اینجا هر کارزار یک بار
 * تعریف می‌شود و لینک‌ها از رویش ساخته می‌شوند، نه برعکس.
 * <p>
 * ⚠️ {@code landingPath} عمداً <b>مسیرِ نسبیِ داخلی</b> است و هرگز نشانیِ کامل.
 * دلیلش بندِ امنیتیِ لینکِ کوتاه است: اگر مقصد بتواند دامنهٔ بیرونی باشد،
 * {@code /l/{code}} به ابزارِ ریدایرکتِ باز تبدیل می‌شود و اعتبارِ دامنهٔ ما خرجِ
 * سایتِ جعلیِ کسِ دیگری می‌شود.
 */
@Document(collection = "campaigns")
public class Campaign {

    @Id
    private String id;

    /** نامِ نمایشیِ فارسی — «کارزارِ نوروز ۱۴۰۵». */
    private String name;

    /** لاتینِ نرمال‌شده و یکتا؛ همین در {@code utm_campaign} می‌رود. */
    private String slug;

    /** کدِ کوتاهِ لینک — ۶ تا ۸ کاراکتر، یکتا، بی‌ابهام. */
    private String code;

    /** از واژگانِ بستهٔ {@code StoreSettings}. */
    private String source;
    private String medium;

    private String term;
    private String content;

    /** مسیرِ داخلی، همیشه با «/» شروع می‌شود. */
    private String landingPath;

    private Instant startsAt;
    private Instant endsAt;

    /** هزینهٔ کارزار به ریال، دستی. بدونِ این، گزارش فقط جالب است نه تصمیم‌ساز. */
    private Long cost;

    private boolean active = true;
    private String notes;

    private Instant createdAt = Instant.now();

    /** آیا همین حالا باید لینکش کار کند؟ */
    public boolean isLive(Instant now) {
        if (!active) return false;
        if (startsAt != null && now.isBefore(startsAt)) return false;
        return endsAt == null || !now.isAfter(endsAt);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getMedium() { return medium; }
    public void setMedium(String medium) { this.medium = medium; }

    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getLandingPath() { return landingPath; }
    public void setLandingPath(String landingPath) { this.landingPath = landingPath; }

    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }

    public Instant getEndsAt() { return endsAt; }
    public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }

    public Long getCost() { return cost; }
    public void setCost(Long cost) { this.cost = cost; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
