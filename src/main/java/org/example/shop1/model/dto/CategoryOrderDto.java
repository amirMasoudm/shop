package org.example.shop1.model.dto;

/**
 * یک آیتم در درخواست مرتب‌سازی/جابجایی دسته‌ها (درگ‌دراپ).
 * فرانت‌اند کل ساختار درخت را به صورت لیستی از این آیتم‌ها می‌فرستد.
 */
public class CategoryOrderDto {

    private String id;
    private String parentId; // null یعنی ریشه
    private Integer position; // ترتیب بین هم‌ردیف‌ها

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
}
