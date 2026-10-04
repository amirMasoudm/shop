package org.example.shop1.model.dto;

import org.example.shop1.model.enums.BannerPlacement;

/**
 * نمایِ آماده‌ی رندر برایِ یک اسلایدِ بنر — {@code resolvedUrl} همین‌جا و همین حالا
 * از رویِ شناسه ساخته شده (نه در زمانِ ذخیره)، تا تغییرِ بعدیِ نامِ محصول/دسته/مقاله
 * لینک را نشکند.
 */
public class BannerDisplayDto {
    private final String id;
    private final String title;          // برایِ اسلایدرِ فروشگاه فقط داخلی است (رندر نمی‌شود)؛
                                          // اسلایدرِ صفحه‌ی اصلی همین را بالایِ بنر نشان می‌دهد.
    private final String imageUrl;
    private final String imageUrlMobile; // همیشه پر است (fallback به imageUrl اعمال شده)
    private final String altText;
    private final String resolvedUrl;    // null یعنی بنر لینک ندارد (linkType=NONE یا هدف پیدا نشد)
    private final boolean external;      // true یعنی target=_blank rel=noopener nofollow
    private final BannerPlacement placement;
    private final String afterSectionId; // فقط وقتی placement=AFTER_SECTION معنا دارد

    public BannerDisplayDto(String id, String title, String imageUrl, String imageUrlMobile, String altText,
                            String resolvedUrl, boolean external,
                            BannerPlacement placement, String afterSectionId) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.imageUrlMobile = imageUrlMobile;
        this.altText = altText;
        this.resolvedUrl = resolvedUrl;
        this.external = external;
        this.placement = placement;
        this.afterSectionId = afterSectionId;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getImageUrl() { return imageUrl; }
    public String getImageUrlMobile() { return imageUrlMobile; }
    public String getAltText() { return altText; }
    public String getResolvedUrl() { return resolvedUrl; }
    public boolean isExternal() { return external; }
    public BannerPlacement getPlacement() { return placement; }
    public String getAfterSectionId() { return afterSectionId; }
}
