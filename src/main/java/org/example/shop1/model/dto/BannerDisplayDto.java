package org.example.shop1.model.dto;

/**
 * نمایِ آماده‌ی رندر برایِ یک اسلایدِ بنر — {@code resolvedUrl} همین‌جا و همین حالا
 * از رویِ شناسه ساخته شده (نه در زمانِ ذخیره)، تا تغییرِ بعدیِ نامِ محصول/دسته/مقاله
 * لینک را نشکند.
 */
public class BannerDisplayDto {
    private final String id;
    private final String imageUrl;
    private final String imageUrlMobile; // همیشه پر است (fallback به imageUrl اعمال شده)
    private final String altText;
    private final String resolvedUrl;    // null یعنی بنر لینک ندارد (linkType=NONE یا هدف پیدا نشد)
    private final boolean external;      // true یعنی target=_blank rel=noopener nofollow

    public BannerDisplayDto(String id, String imageUrl, String imageUrlMobile, String altText,
                            String resolvedUrl, boolean external) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.imageUrlMobile = imageUrlMobile;
        this.altText = altText;
        this.resolvedUrl = resolvedUrl;
        this.external = external;
    }

    public String getId() { return id; }
    public String getImageUrl() { return imageUrl; }
    public String getImageUrlMobile() { return imageUrlMobile; }
    public String getAltText() { return altText; }
    public String getResolvedUrl() { return resolvedUrl; }
    public boolean isExternal() { return external; }
}
