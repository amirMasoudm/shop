package org.example.shop1.model.enums;

/**
 * کجا این بنر نمایش داده شود.
 * {@code HERO}: اسلایدرِ اصلیِ بالایِ صفحه‌ی فروشگاه (/shop) — LCP، کاملاً SSR.
 * {@code AFTER_SECTION}: بلافاصله زیرِ یک سکشنِ لندینگِ مشخص در /shop ({@code Banner.afterSectionId})؛
 * چون این سکشن‌ها خودشان سمتِ کلاینت (JS) رندر می‌شوند، این بنرها هم همان‌جا رندر می‌شوند، نه SSR.
 * {@code HOME_HERO}: اسلایدرِ بالایِ صفحه‌ی داده‌نما (ریشه‌ی سایت، {@code /}) — همان سیستم،
 * جایگاهِ جدا، چون دو صفحه‌ی متفاوت‌اند.
 */
public enum BannerPlacement {
    HERO,
    AFTER_SECTION,
    HOME_HERO
}
