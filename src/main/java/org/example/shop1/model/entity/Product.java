package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "products")
public class Product {

    @Id
    private String id; // شناسه محصول که می‌تواند توسط ادمین وارد شود و باید یونیک باشد
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String categoryId;

    // --- فیلدهای سورتینگ و نمایش در فرانت‌اند ---
    private BigDecimal discountedPrice; // قیمت پس از اعمال تخفیف
    private Integer discountPercent = 0; // درصد تخفیف (0 تا 100)
    private Double averageRating = 0.0; // میانگین امتیاز
    private Long reviewCount = 0L; // تعداد کل امتیازات داده شده
    private Long salesCount = 0L; // تعداد فروش موفق (برای سورت پرفروش‌ترین)
    private Instant createdAt = Instant.now(); // تاریخ و زمان ایجاد (برای سورت جدیدترین)
    // ------------------------------------------

    private List<String> images = new ArrayList<>();
    private Map<String, String> specifications = new HashMap<>();


    // در کلاس Product این فیلد را اضافه کنید
    private String warehouseCategoryId; // دسته‌بندی مختص انبار/فروشگاه فیزیکی
    // اضافه کردن قیمت پایه (فی)
    private BigDecimal basePrice;

    // اضافه کردن زمان آخرین بروزرسانی
    private Instant updatedAt;
    // فیلد جدید برای توضیحات داخلی انبار
    private String warehouseDescription;

    // ... سایر فیلدها ...

    private String unit; // واحد سنجش (عدد، کیلوگرم، بسته و...)
    private Integer packQuantity; // تعداد در بسته (فقط اگر واحد "بسته" باشد پر می‌شود)
    // استیکرها / سکشن‌های متصل به این محصول برای نمایش در لندینگ
    private List<String> sectionIds = new ArrayList<>();

    private BigDecimal onlinePrice; // قیمت فروش سایت

    // در فایل Product.java این فیلدها را به بدنه کلاس اضافه کنید:

    private Double weight; // به گرم
    private Double length; // به سانتی‌متر
    private Double width;  // به سانتی‌متر
    private Double height; // به سانتی‌متر

    // ==========================================
    // فیلدهای جدید مربوط به SEO
    // ==========================================
    private String slug;
    private String seoTitle;
    private String seoDescription;

    // جدول مشخصات فنی گروه‌بندی‌شده (جدا از specifications فیلترپذیر)
    private List<TechSpecRow> techSpecs = new ArrayList<>();

    // پرسش‌های متداول محصول (اسکیمای FAQPage از رویش ساخته می‌شود)
    private List<FaqItem> faqs = new ArrayList<>();

    // محصولات مکمل/مرتبط دستی (انتخاب ادمین)
    private List<String> relatedProductIds = new ArrayList<>();

    public List<TechSpecRow> getTechSpecs() { return techSpecs; }
    public void setTechSpecs(List<TechSpecRow> techSpecs) { this.techSpecs = techSpecs; }

    public List<FaqItem> getFaqs() { return faqs; }
    public void setFaqs(List<FaqItem> faqs) { this.faqs = faqs; }

    public List<String> getRelatedProductIds() { return relatedProductIds; }
    public void setRelatedProductIds(List<String> relatedProductIds) { this.relatedProductIds = relatedProductIds; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getSeoTitle() { return seoTitle; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; }

    public String getSeoDescription() { return seoDescription; }
    public void setSeoDescription(String seoDescription) { this.seoDescription = seoDescription; }
    // ==========================================

    // گترها و سترها:
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public Double getLength() { return length; }
    public void setLength(Double length) { this.length = length; }

    public Double getWidth() { return width; }
    public void setWidth(Double width) { this.width = width; }

    public Double getHeight() { return height; }
    public void setHeight(Double height) { this.height = height; }

    public BigDecimal getOnlinePrice() { return onlinePrice; }
    public void setOnlinePrice(BigDecimal onlinePrice) { this.onlinePrice = onlinePrice; }

    public List<String> getSectionIds() { return sectionIds; }
    public void setSectionIds(List<String> sectionIds) { this.sectionIds = sectionIds; }
    // Getters & Setters
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Integer getPackQuantity() { return packQuantity; }
    public void setPackQuantity(Integer packQuantity) { this.packQuantity = packQuantity; }

    public String getWarehouseDescription() { return warehouseDescription; }
    public void setWarehouseDescription(String warehouseDescription) { this.warehouseDescription = warehouseDescription; }

    // --- Getters & Setters ---
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    // Getter و Setter
    public String getWarehouseCategoryId() { return warehouseCategoryId; }
    public void setWarehouseCategoryId(String warehouseCategoryId) { this.warehouseCategoryId = warehouseCategoryId; }


    public Product() {}

    // --- Getters & Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public Map<String, String> getSpecifications() { return specifications; }
    public void setSpecifications(Map<String, String> specifications) { this.specifications = specifications; }

    // Getters & Setters فیلدهای جدید
    public BigDecimal getDiscountedPrice() { return discountedPrice; }
    public void setDiscountedPrice(BigDecimal discountedPrice) { this.discountedPrice = discountedPrice; }

    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Long getReviewCount() { return reviewCount; }
    public void setReviewCount(Long reviewCount) { this.reviewCount = reviewCount; }

    public Long getSalesCount() { return salesCount; }
    public void setSalesCount(Long salesCount) { this.salesCount = salesCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}