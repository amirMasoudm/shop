package org.example.shop1.model.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ProductSearchRequestDto {
    private String categoryId;
    private String searchQuery; // برای سرچ متنی (نام محصول)
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    // مپ فیلترهای داینامیک. مثال: {"رنگ": ["قرمز", "آبی"], "سایز": ["XL"]}
    private Map<String, List<String>> specifications;

    private String sortBy = "createdAt"; // createdAt, priceAsc, priceDesc, sales
    private int page = 0;
    private int size = 20;

    // Getters & Setters
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }
    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }
    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }
    public Map<String, List<String>> getSpecifications() { return specifications; }
    public void setSpecifications(Map<String, List<String>> specifications) { this.specifications = specifications; }
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}