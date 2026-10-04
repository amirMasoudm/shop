package org.example.shop1.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * بدنهٔ ساخت/ویرایشِ دسته.
 * <p>
 * 🔴 <b>تشخیصِ «نیامده» از «صریحاً null».</b> {@code PUT} ادغامی است، نه جایگزینی:
 * فیلدی که در بدنه نیامده دست نمی‌خورد، فیلدی که با {@code null} آمده پاک می‌شود، و
 * فیلدی که با مقدار آمده ست می‌شود. بدونِ این تفکیک، یک اسکریپتِ ساده که فقط نام را
 * عوض می‌کرد سئو و رنگ و filterKeys را پاک می‌کرد — و بدتر، چون {@code parentId}
 * نیامده با {@code null} یکی بود، هر دستهٔ غیرریشه را به ریشه می‌برد.
 * <p>
 * سازوکار: Jackson ستِرِ هر فیلد را <b>فقط وقتی آن کلید در JSON آمده باشد</b> صدا
 * می‌زند — حتی اگر مقدارش {@code null} باشد — و برای کلیدِ غایب اصلاً صدا نمی‌زند.
 * پس هر ستِر نامِ فیلدش را ثبت می‌کند و سرویس با {@link #has} می‌پرسد.
 * <p>
 * ⚠️ مجموعهٔ ثبت عمداً گتر ندارد: Jackson مجموعه‌ای را که گتر داشته باشد می‌تواند از
 * بیرون پر کند، و آن‌وقت کلاینت خودش تعیین می‌کرد چه چیزی «آمده» حساب شود.
 * ⚠️ نامِ فیلد در {@code mark} و در {@code has} باید دقیقاً همان کلیدِ JSON باشد؛
 * {@code CategoryUpdateMergeTest} این را با یک ObjectMapperِ واقعی می‌سنجد.
 */
public class CategoryRequestDto {

    @JsonIgnore
    private final Set<String> present = new HashSet<>();

    private void mark(String field) { present.add(field); }

    /** آیا این کلید در بدنهٔ درخواست آمده بود (حتی با {@code null})؟ */
    public boolean has(String field) { return present.contains(field); }

    private String name;
    private String parentId;
    private String newParentId;
    // *** این فیلد حیاتی است ***
    private String type;

    // --- Getters & Setters ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; mark("name"); }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; mark("parentId"); }

    public String getNewParentId() { return newParentId; }
    public void setNewParentId(String newParentId) { this.newParentId = newParentId; mark("newParentId"); }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; mark("type"); }

    // ترتیبِ نمایش دستی — اختیاری؛ اگر ندهید مثلِ قبل به انتهای هم‌ردیف‌ها اضافه می‌شود
    private Integer position;
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; mark("position"); }

    // ترتیبِ نوارِ چیپیِ موبایل؛ null یعنی «همان ترتیبِ منو»
    private Integer stripPosition;
    public Integer getStripPosition() { return stripPosition; }
    public void setStripPosition(Integer stripPosition) { this.stripPosition = stripPosition; mark("stripPosition"); }
    // اضافه کردن به DTO ورودی
    private List<String> filterKeys;

    public List<String> getFilterKeys() { return filterKeys; }
    public void setFilterKeys(List<String> filterKeys) { this.filterKeys = filterKeys; mark("filterKeys"); }

    // ---> فیلدهای سئو دسته <---
    private String slug;
    private String seoTitle;
    private String seoDescription;
    private String introText;

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; mark("slug"); }

    public String getSeoTitle() { return seoTitle; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; mark("seoTitle"); }

    public String getSeoDescription() { return seoDescription; }
    public void setSeoDescription(String seoDescription) { this.seoDescription = seoDescription; mark("seoDescription"); }

    public String getIntroText() { return introText; }
    public void setIntroText(String introText) { this.introText = introText; mark("introText"); }

    // رنگِ دسته — null/خالی یعنی بی‌رنگ (ارث‌بری از والد)
    private String color;
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; mark("color"); }

    // نامِ سازنده برایِ اسکیمای محصول — خالی یعنی «نمی‌دانیم»، نه «بی‌برند»
    private String brandName;
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; mark("brandName"); }
}
