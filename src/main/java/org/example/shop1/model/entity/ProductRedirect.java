package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * نگاشتِ «اسلاگِ قدیمی → محصولِ زنده» برایِ ۳۰۱ دادن به‌جایِ ۴۰۴.
 * <p>
 * ⚠️ <b>تغییرِ schema:</b> کالکشنِ جدیدِ {@code product_redirects}.
 * <p>
 * <b>چرا لازم شد:</b> وقتی دو رکوردِ تکراریِ یک محصول ادغام می‌شوند، رکوردِ بازنده حذف
 * می‌شود ولی اسلاگش در {@code sitemap.xml} ایندکس شده است. بدونِ این کالکشن،
 * {@code resolveProduct} چیزی پیدا نمی‌کند و مستقیم ۴۰۴ می‌دهد — که چارچوبِ سئویِ
 * پروژه صریح ممنوع کرده («هرگز ۴۰۴؛ ۳۰۱ به جایگزین»).
 * <p>
 * <b>چرا {@code toResolver} به‌جایِ شناسهٔ خام:</b> این فیلد همان چیزی را نگه می‌دارد که
 * {@code ProductUrlUtil.productResolver} تولید می‌کند — اسلاگ اگر باشد، وگرنه شناسه.
 * دلیلش زنجیره است: اگر محصولِ مقصد خودش بعداً ادغام شود، ریدایرکتِ تازه با
 * <i>اسلاگِ</i> او کلید می‌خورد؛ اگر اینجا شناسهٔ خام ذخیره کرده بودیم زنجیره پاره می‌شد
 * و A→B→C به ۴۰۴ می‌رسید.
 */
@Document(collection = "product_redirects")
public class ProductRedirect {

    @Id
    private String id;

    /** اسلاگِ محصولِ حذف‌شده (کلیدِ جست‌وجو). یکتا — دو مقصد برای یک اسلاگ بی‌معناست. */
    @Indexed(unique = true)
    private String fromSlug;

    /** resolverِ محصولِ مقصد: اسلاگ اگر داشته باشد، وگرنه شناسه. */
    private String toResolver;

    /** یادداشتِ اختیاری — مثلاً «ادغامِ خوشهٔ hEX، رکوردِ B». */
    private String note;

    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFromSlug() { return fromSlug; }
    public void setFromSlug(String fromSlug) { this.fromSlug = fromSlug; }

    public String getToResolver() { return toResolver; }
    public void setToResolver(String toResolver) { this.toResolver = toResolver; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
