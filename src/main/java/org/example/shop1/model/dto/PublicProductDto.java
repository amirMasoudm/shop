package org.example.shop1.model.dto;

import org.example.shop1.model.entity.FaqItem;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.entity.TechSpecRow;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * نمایِ عمومیِ محصول — چیزی که به مرورگرِ کاربر و هر مصرف‌کنندهٔ بیرونی می‌رود.
 * <p>
 * <b>چرا این کلاس هست:</b> اندپوینت‌هایِ عمومی قبلاً کلِ انتیتیِ {@code Product} را
 * سریالایز می‌کردند، یعنی {@code basePrice} (قیمتِ خرید)، {@code warehouseDescription}
 * و {@code salesCount} برایِ همه قابلِ‌دیدن بود. با فیلدهایِ قیمت‌گذاریِ در راه
 * (قیمتِ همکار، کفِ قیمتِ رقبا) این نشت به‌مراتب حساس‌تر می‌شد.
 * <p>
 * <b>قاعده:</b> این کلاس allow-list است، نه deny-list. فیلدِ جدیدِ {@code Product}
 * به‌طورِ پیش‌فرض عمومی <i>نمی‌شود</i> — فقط اگر عمداً اینجا اضافه شود. برایِ فیلدِ
 * داخلیِ جدید هیچ کاری لازم نیست.
 * <p>
 * پنلِ ادمین ({@code /api/v1/products/admin}) و APIِ ترب عمداً از این مسیر رد نمی‌شوند:
 * اولی احرازِ هویتِ ادمین دارد و دومی خودش نگاشتِ صریحِ فیلد دارد.
 */
public class PublicProductDto {

    private final String id;
    private final String name;
    private final String description;
    private final String slug;

    // قیمت‌هایِ نمایشی؛ basePrice (قیمتِ خرید) عمداً اینجا نیست
    private final BigDecimal price;
    private final BigDecimal onlinePrice;
    private final Integer discountPercent;

    private final Integer stock;
    private final String categoryId;
    private final List<String> sectionIds;

    private final List<String> images;
    private final List<String> imageAlts;

    private final Map<String, String> specifications;
    private final List<TechSpecRow> techSpecs;
    private final List<FaqItem> faqs;
    private final List<String> relatedProductIds;

    private final String seoTitle;
    private final String seoDescription;

    private final Double averageRating;
    private final Long reviewCount;

    // توقفِ تولید — فروشگاهِ SPA با این‌ها تصمیم می‌گیرد که به جایگزین برود یا
    // صفحهٔ متوقف‌شده را نشان دهد. شناسهٔ جایگزین عمومی است، چون خودِ آن محصول
    // هم عمومی است.
    private final boolean discontinued;
    private final String replacementProductId;
    private final boolean discontinuedNoticeVisible;

    public PublicProductDto(Product p) {
        this.id = p.getId();
        this.name = p.getName();
        this.description = p.getDescription();
        this.slug = p.getSlug();
        this.price = p.getPrice();
        this.onlinePrice = p.getOnlinePrice();
        this.discountPercent = p.getDiscountPercent();
        this.stock = p.getStock();
        this.categoryId = p.getCategoryId();
        this.sectionIds = p.getSectionIds();
        this.images = p.getImages();
        this.imageAlts = p.getImageAlts();
        this.specifications = p.getSpecifications();
        this.techSpecs = p.getTechSpecs();
        this.faqs = p.getFaqs();
        this.relatedProductIds = p.getRelatedProductIds();
        this.seoTitle = p.getSeoTitle();
        this.seoDescription = p.getSeoDescription();
        this.averageRating = p.getAverageRating();
        this.reviewCount = p.getReviewCount();
        this.discontinued = p.isProductionStopped();
        this.replacementProductId = p.isProductionStopped() ? p.getReplacementProductId() : null;
        this.discontinuedNoticeVisible = p.isDiscontinuedNoticeShown();
    }

    public static PublicProductDto of(Product p) {
        return new PublicProductDto(p);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getSlug() { return slug; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getOnlinePrice() { return onlinePrice; }
    public Integer getDiscountPercent() { return discountPercent; }
    public Integer getStock() { return stock; }
    public String getCategoryId() { return categoryId; }
    public List<String> getSectionIds() { return sectionIds; }
    public List<String> getImages() { return images; }
    public List<String> getImageAlts() { return imageAlts; }
    public Map<String, String> getSpecifications() { return specifications; }
    public List<TechSpecRow> getTechSpecs() { return techSpecs; }
    public List<FaqItem> getFaqs() { return faqs; }
    public List<String> getRelatedProductIds() { return relatedProductIds; }
    public String getSeoTitle() { return seoTitle; }
    public String getSeoDescription() { return seoDescription; }
    public Double getAverageRating() { return averageRating; }
    public Long getReviewCount() { return reviewCount; }
    public boolean isDiscontinued() { return discontinued; }
    public String getReplacementProductId() { return replacementProductId; }
    public boolean isDiscontinuedNoticeVisible() { return discontinuedNoticeVisible; }
}
