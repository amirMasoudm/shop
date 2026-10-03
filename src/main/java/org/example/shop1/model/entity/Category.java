package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "categories")
public class Category {
    public Category() {}

    @Id
    private String id;
    private String name;
    private String parentId; // nullable
    private String type; // ONLINE یا WAREHOUSE (فیلد جدید)

    private List<String> ancestors = new ArrayList<>();
    private Integer level = 0;
    private List<String> childrenIds = new ArrayList<>();

    // ترتیب نمایش بین هم‌ردیف‌ها (برای درگ‌دراپ و مرتب‌سازی پایدار)
    private Integer position = 0;

    /**
     * ترتیبِ همین دسته در نوارِ چیپیِ زیرِ جست‌وجویِ موبایل — جدا از {@link #position}.
     * <p>
     * خواستهٔ صریحِ مالک: منویِ فروشگاه (نوارِ دسکتاپ و کشویِ موبایل) یک ایندکس
     * داشته باشد و آن نوار یک ایندکسِ دیگر، چون کارکردشان یکی نیست: منو فهرستِ
     * کامل است و نوار میان‌بُرِ پرکاربردها.
     * <p>
     * {@code null} یعنی «جدا تنظیم نشده» و مصرف‌کننده به {@code position} برمی‌گردد،
     * پس دسته‌های موجود بدونِ مهاجرت سرِ جای خودشان می‌مانند.
     */
    private Integer stripPosition;

    // ---> فیلدهای سئو برای صفحه‌ی مستقل دسته (/category/{slug}) <---
    private String slug;           // آدرس تمیز و یکتا
    private String seoTitle;       // عنوان تگ <title> و og:title
    private String seoDescription; // متن meta description
    private String introText;      // متنِ معرفیِ ۲۰۰–۴۰۰ کلمه‌ای دسته (رندرِ SSR زیرِ لیستِ محصولات؛ nullable)

    /**
     * رنگِ شناسهٔ دسته — نوارِ باریکِ کنارِ ردیفِ محصول در میزِ کار از این می‌آید.
     * <p>
     * {@code null} یا رشتهٔ خالی یعنی «بی‌رنگ»؛ در آن حالت مصرف‌کننده رنگِ
     * دستهٔ والد را به‌کار می‌برد و اگر هیچ نیایی رنگ نداشت، هیچ نواری کشیده
     * نمی‌شود. سفید ({@code #ffffff}) عمداً یک رنگِ معتبر است و با بی‌رنگ
     * اشتباه نمی‌شود — پس «سفید» انتخابی است، نه نبودِ انتخاب.
     */
    private String color;

    /**
     * نامِ سازندهٔ واقعی برایِ دادهٔ ساختاریافته — مثلاً {@code Ubiquiti} رویِ دستهٔ
     * یوبیکیوتی.
     * <p>
     * 🔴 <b>چرا فیلدِ داده و نه نگاشتی در کد:</b> فهرستِ برندها مخصوصِ همین فروشگاه
     * است و در یک اپِ قابلِ‌استفادهٔ مجدد نباید هاردکد شود. محصول برندش را از نزدیک‌ترین
     * نیایی می‌گیرد که این فیلد را دارد.
     * <p>
     * ⚠️ نامِ خودِ دسته جایگزینش نیست: «یوبیکیوتی (UBNT)» نامِ نمایشیِ فارسی است، ولی
     * گوگل نامِ سازنده را می‌خواهد. و {@code null} یعنی «نمی‌دانیم» — در آن حالت فیلدِ
     * {@code brand} در اسکیما اصلاً نوشته نمی‌شود، چون برندِ غلط از نبودنِ برند بدتر است.
     */
    private String brandName;

    // --- Constructors ---
    // اضافه کردن فیلد جدید
    private List<String> filterKeys = new ArrayList<>(); // مثلا: ["سایز", "رنگ", "ولتاژ"]

    // Getter & Setter
    public List<String> getFilterKeys() { return filterKeys; }
    public void setFilterKeys(List<String> filterKeys) { this.filterKeys = filterKeys; }

    // --- Getters & Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<String> getAncestors() { return ancestors; }
    public void setAncestors(List<String> ancestors) { this.ancestors = ancestors; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public List<String> getChildrenIds() { return childrenIds; }
    public void setChildrenIds(List<String> childrenIds) { this.childrenIds = childrenIds; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public Integer getStripPosition() { return stripPosition; }
    public void setStripPosition(Integer stripPosition) { this.stripPosition = stripPosition; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getSeoTitle() { return seoTitle; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; }

    public String getSeoDescription() { return seoDescription; }
    public void setSeoDescription(String seoDescription) { this.seoDescription = seoDescription; }

    public String getIntroText() { return introText; }
    public void setIntroText(String introText) { this.introText = introText; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
}