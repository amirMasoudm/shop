package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "landing_sections")
public class LandingSection {

    @Id
    private String id;
    private String title; // مثال: "جشنواره آخر سال" یا "پرفروش‌ترین‌ها"

    // تنظیمات استیکر
    private String stickerText; // متن استیکر (مثلاً "-50%")
    private String stickerColor; // کد رنگ استیکر (مثلاً "#ff0000") یا آدرس عکس استیکر

    private int orderIndex = 0; // ترتیب نمایش در صفحه لندینگ
    private boolean isActive = true; // فعال/غیرفعال بودن سکشن

    // انتخاب‌های ادمین
    private List<String> selectedCategoryIds = new ArrayList<>(); // دسته‌های انتخاب شده
    private List<String> selectedProductIds = new ArrayList<>(); // محصولات انتخاب شده (تکی)

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStickerText() { return stickerText; }
    public void setStickerText(String stickerText) { this.stickerText = stickerText; }
    public String getStickerColor() { return stickerColor; }
    public void setStickerColor(String stickerColor) { this.stickerColor = stickerColor; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public List<String> getSelectedCategoryIds() { return selectedCategoryIds; }
    public void setSelectedCategoryIds(List<String> selectedCategoryIds) { this.selectedCategoryIds = selectedCategoryIds; }
    public List<String> getSelectedProductIds() { return selectedProductIds; }
    public void setSelectedProductIds(List<String> selectedProductIds) { this.selectedProductIds = selectedProductIds; }
}