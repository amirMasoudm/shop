package org.example.shop1.model.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ProductRequest {

    private String id; // شناسه محصول (Product Code) که ادمین وارد می‌کند
    private String name;
    private String categoryId;
    private BigDecimal price;
    private Integer stock;
    private String description;
    private Map<String, String> specifications;
    private List<String> images;
    private List<String> imageAlts; // altِ اختصاصیِ تصاویر (هم‌ترتیبِ images)
    private Integer discountPercent; // درصد تخفیف ارسالی از پنل ادمین


    // به DTO هم اضافه کنید تا از فرانت دریافت شود
    private String warehouseCategoryId;
    // دریافت قیمت پایه از فرانت
    private BigDecimal basePrice;
    private String warehouseDescription;
// ... سایر فیلدها ...

    private String unit;
    private Integer packQuantity;

    private BigDecimal onlinePrice;

    // در فایل ProductRequest.java این فیلدها را اضافه کنید:

    private Double weight;
    private Double length;
    private Double width;
    private Double height;

    // ==========================================
    // فیلدهای جدید مربوط به SEO
    // ==========================================
    private String slug;
    private String persianSlug; // دُمِ فارسیِ آدرسِ هیبرید (اختیاری)
    private String seoTitle;
    private String seoDescription;

    // جدول مشخصات فنی + پرسش‌های متداول + محصولات مرتبط
    private java.util.List<org.example.shop1.model.entity.TechSpecRow> techSpecs;
    private java.util.List<org.example.shop1.model.entity.FaqItem> faqs;
    private java.util.List<String> relatedProductIds;

    public java.util.List<org.example.shop1.model.entity.TechSpecRow> getTechSpecs() { return techSpecs; }
    public void setTechSpecs(java.util.List<org.example.shop1.model.entity.TechSpecRow> techSpecs) { this.techSpecs = techSpecs; }

    public java.util.List<org.example.shop1.model.entity.FaqItem> getFaqs() { return faqs; }
    public void setFaqs(java.util.List<org.example.shop1.model.entity.FaqItem> faqs) { this.faqs = faqs; }

    public java.util.List<String> getRelatedProductIds() { return relatedProductIds; }
    public void setRelatedProductIds(java.util.List<String> relatedProductIds) { this.relatedProductIds = relatedProductIds; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getPersianSlug() { return persianSlug; }
    public void setPersianSlug(String persianSlug) { this.persianSlug = persianSlug; }

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


    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Integer getPackQuantity() { return packQuantity; }
    public void setPackQuantity(Integer packQuantity) { this.packQuantity = packQuantity; }


    public String getWarehouseDescription() { return warehouseDescription; }
    public void setWarehouseDescription(String warehouseDescription) { this.warehouseDescription = warehouseDescription; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public String getWarehouseCategoryId() { return warehouseCategoryId; }
    public void setWarehouseCategoryId(String warehouseCategoryId) { this.warehouseCategoryId = warehouseCategoryId; }


    // --- Getters & Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    // ===== میزِ کارِ قیمت‌گذاری / ورودِ دسته‌ای =====
    // موجودیِ تفکیکیِ شعب و «در راه». اگر شعبه‌ها فرستاده شوند، stock از رویشان
    // محاسبه می‌شود (اصفهان + تهران) و «در راه» عمداً در آن شمرده نمی‌شود.
    private Integer stockIsfahan;
    private Integer stockTehran;
    private Integer incomingStock;
    private BigDecimal partnerUnitPrice;
    private BigDecimal partnerBulkPrice;
    private BigDecimal dollarPrice;
    private Boolean pushSaleFlag;
    private BigDecimal torobFloorPrice;
    private String torobUrl;
    private BigDecimal digikalaFloorPrice;
    private String digikalaUrl;

    public Integer getStockIsfahan() { return stockIsfahan; }
    public void setStockIsfahan(Integer stockIsfahan) { this.stockIsfahan = stockIsfahan; }

    public Integer getStockTehran() { return stockTehran; }
    public void setStockTehran(Integer stockTehran) { this.stockTehran = stockTehran; }

    public Integer getIncomingStock() { return incomingStock; }
    public void setIncomingStock(Integer incomingStock) { this.incomingStock = incomingStock; }

    public BigDecimal getPartnerUnitPrice() { return partnerUnitPrice; }
    public void setPartnerUnitPrice(BigDecimal partnerUnitPrice) { this.partnerUnitPrice = partnerUnitPrice; }

    public BigDecimal getPartnerBulkPrice() { return partnerBulkPrice; }
    public void setPartnerBulkPrice(BigDecimal partnerBulkPrice) { this.partnerBulkPrice = partnerBulkPrice; }

    public BigDecimal getDollarPrice() { return dollarPrice; }
    public void setDollarPrice(BigDecimal dollarPrice) { this.dollarPrice = dollarPrice; }

    public Boolean getPushSaleFlag() { return pushSaleFlag; }
    public void setPushSaleFlag(Boolean pushSaleFlag) { this.pushSaleFlag = pushSaleFlag; }

    public BigDecimal getTorobFloorPrice() { return torobFloorPrice; }
    public void setTorobFloorPrice(BigDecimal torobFloorPrice) { this.torobFloorPrice = torobFloorPrice; }

    public String getTorobUrl() { return torobUrl; }
    public void setTorobUrl(String torobUrl) { this.torobUrl = torobUrl; }

    public BigDecimal getDigikalaFloorPrice() { return digikalaFloorPrice; }
    public void setDigikalaFloorPrice(BigDecimal digikalaFloorPrice) { this.digikalaFloorPrice = digikalaFloorPrice; }

    public String getDigikalaUrl() { return digikalaUrl; }
    public void setDigikalaUrl(String digikalaUrl) { this.digikalaUrl = digikalaUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, String> getSpecifications() { return specifications; }
    public void setSpecifications(Map<String, String> specifications) { this.specifications = specifications; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public List<String> getImageAlts() { return imageAlts; }
    public void setImageAlts(List<String> imageAlts) { this.imageAlts = imageAlts; }

    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }
}