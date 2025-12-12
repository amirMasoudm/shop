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