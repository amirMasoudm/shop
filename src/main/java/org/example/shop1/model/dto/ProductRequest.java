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
    private Integer discountPercent; // درصد تخفیف ارسالی از پنل ادمین

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

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, String> getSpecifications() { return specifications; }
    public void setSpecifications(Map<String, String> specifications) { this.specifications = specifications; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }
}