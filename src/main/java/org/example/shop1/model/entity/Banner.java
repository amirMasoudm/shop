package org.example.shop1.model.entity;

import org.example.shop1.model.enums.BannerLinkType;
import org.example.shop1.model.enums.BannerPlacement;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * یک اسلایدِ بنرِ صفحه‌ی خانه. طبقِ docs/prompt-tech-chat-home-banner-slider.md —
 * موجودیتِ جداست، نه توسعه‌ی {@link LandingSection} (آن ویترینِ محصول/دسته است، عکسِ آزاد ندارد).
 * <p>
 * 🔴 برایِ {@code linkType}هایِ PRODUCT/CATEGORY/ARTICLE، {@code linkTarget} همیشه
 * <b>شناسه</b> است، نه آدرسِ کامل — آدرسِ نهایی هر بار سمتِ سرور ساخته می‌شود
 * (BannerService.resolveUrl) چون آدرسِ محصولاتِ این پروژه هیبریدِ فارسی است و با
 * تغییرِ نامِ محصول عوض می‌شود.
 */
@Document(collection = "banners")
public class Banner {

    @Id
    private String id;

    // عنوانِ داخلی — فقط برایِ خودِ ادمین، رویِ سایت رندر نمی‌شود
    private String title;

    private String imageUrl;

    // اختیاری — اگر خالی بود، از imageUrl استفاده می‌شود (BannerService این fallback را اعمال می‌کند)
    private String imageUrlMobile;

    // اجباری — برایِ سئو و دسترس‌پذیری (اعتبارسنجی سمتِ سرویس)
    private String altText;

    private BannerLinkType linkType = BannerLinkType.NONE;

    // بسته به linkType: شناسه‌ی محصول/دسته/مقاله، یا مسیرِ داخلی (/about)، یا آدرسِ کاملِ خارجی
    private String linkTarget;

    private int orderIndex = 0;
    private boolean isActive = true;

    // کجایِ صفحه‌ی خانه نمایش داده شود؛ پیش‌فرض HERO یعنی همون اسلایدرِ اصلی (رفتارِ اولیه)
    private BannerPlacement placement = BannerPlacement.HERO;

    // فقط وقتی placement=AFTER_SECTION پر می‌شود: شناسه‌ی سکشنِ لندینگی که این بنر زیرِ آن بیاید
    private String afterSectionId;

    public Banner() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getImageUrlMobile() { return imageUrlMobile; }
    public void setImageUrlMobile(String imageUrlMobile) { this.imageUrlMobile = imageUrlMobile; }

    public String getAltText() { return altText; }
    public void setAltText(String altText) { this.altText = altText; }

    public BannerLinkType getLinkType() { return linkType; }
    public void setLinkType(BannerLinkType linkType) { this.linkType = linkType; }

    public String getLinkTarget() { return linkTarget; }
    public void setLinkTarget(String linkTarget) { this.linkTarget = linkTarget; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public BannerPlacement getPlacement() { return placement; }
    public void setPlacement(BannerPlacement placement) { this.placement = placement; }

    public String getAfterSectionId() { return afterSectionId; }
    public void setAfterSectionId(String afterSectionId) { this.afterSectionId = afterSectionId; }
}
