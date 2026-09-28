package org.example.shop1.model.dto;

import java.util.ArrayList;
import java.util.List;

public class CategoryResponseDto {
    private String id;
    private String name;
    private String parentId;
    private String type;
    private Integer level;
    private Integer position;
    // *** این فیلد را اضافه کنید ***
    private List<String> filterKeys = new ArrayList<>();

    private List<CategoryResponseDto> children = new ArrayList<>();

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    // ترتیبِ نوارِ چیپیِ موبایل. جایگزینی با position عمداً سمتِ مصرف‌کننده است،
    // نه این‌جا — تا «ترتیبِ خودش» از «ترتیبِ ارثی» قابلِ تفکیک بماند.
    private Integer stripPosition;
    public Integer getStripPosition() { return stripPosition; }
    public void setStripPosition(Integer stripPosition) { this.stripPosition = stripPosition; }

    // *** Getter & Setter جدید ***
    public List<String> getFilterKeys() { return filterKeys; }
    public void setFilterKeys(List<String> filterKeys) { this.filterKeys = filterKeys; }

    public List<CategoryResponseDto> getChildren() { return children; }
    public void setChildren(List<CategoryResponseDto> children) { this.children = children; }

    // ---> فیلدهای سئو دسته <---
    private String slug;
    private String seoTitle;
    private String seoDescription;
    private String introText;

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getSeoTitle() { return seoTitle; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; }

    public String getSeoDescription() { return seoDescription; }
    public void setSeoDescription(String seoDescription) { this.seoDescription = seoDescription; }

    public String getIntroText() { return introText; }
    public void setIntroText(String introText) { this.introText = introText; }

    // رنگِ دسته. ارث‌بری از والد سمتِ مصرف‌کننده حساب می‌شود، نه اینجا — چون
    // درخت همان‌جا در دست است و این‌طور «رنگِ خودش» از «رنگِ ارثی» جدا می‌ماند.
    private String color;
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    /** نامِ سازنده — پنل با همین فیلد ویرایشش می‌کند و اسکیمای محصول از آن می‌خواند. */
    private String brandName;
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
}