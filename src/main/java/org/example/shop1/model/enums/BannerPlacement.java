package org.example.shop1.model.enums;

/**
 * کجایِ صفحه‌ی خانه این بنر نمایش داده شود.
 * {@code HERO}: اسلایدرِ اصلیِ بالایِ صفحه (LCP، کاملاً SSR — رفتارِ اولیه‌ی این فیچر).
 * {@code AFTER_SECTION}: بلافاصله زیرِ یک سکشنِ لندینگِ مشخص ({@code Banner.afterSectionId})؛
 * چون این سکشن‌ها خودشان سمتِ کلاینت (JS) رندر می‌شوند، این بنرها هم همان‌جا رندر می‌شوند، نه SSR.
 */
public enum BannerPlacement {
    HERO,
    AFTER_SECTION
}
