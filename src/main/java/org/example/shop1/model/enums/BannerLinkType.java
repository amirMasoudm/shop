package org.example.shop1.model.enums;

/**
 * نوعِ لینکِ یک بنرِ اسلایدرِ صفحه‌ی خانه.
 * برایِ {@code PRODUCT}/{@code CATEGORY}/{@code ARTICLE}، {@code Banner.linkTarget} همیشه
 * شناسه است، نه آدرس — آدرسِ نهایی هر بار سمتِ سرور از رویِ شناسه ساخته می‌شود
 * (رجوع به BannerService.resolveUrl) تا با تغییرِ نامِ محصول/دسته/مقاله نشکند.
 */
public enum BannerLinkType {
    NONE,
    PRODUCT,
    CATEGORY,
    ARTICLE,
    INTERNAL_PATH,
    EXTERNAL_URL
}
