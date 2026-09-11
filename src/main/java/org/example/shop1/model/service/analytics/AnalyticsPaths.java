package org.example.shop1.model.service.analytics;

/**
 * مسیرهایی که رویدادِ رفتاری ندارند.
 * <p>
 * اگر استاتیک‌ها مستثنا نشوند، هر لودِ صفحه ده‌ها {@code PAGE_VIEW}ِ بی‌معنی می‌سازد
 * (css، js، فونت، عکس) و هم دیتابیس را پر می‌کند هم عددِ «بازدید» را بی‌معنی.
 */
public final class AnalyticsPaths {

    private static final String[] STATIC_PREFIXES = {
            "/css/", "/js/", "/img/", "/images/", "/fonts/", "/uploads/", "/fragments/", "/ws/"
    };

    private static final String[] STATIC_EXACT = {
            "/favicon.ico", "/robots.txt", "/sitemap.xml", "/error"
    };

    private AnalyticsPaths() {}

    public static boolean isStaticAsset(String uri) {
        if (uri == null) return true;
        for (String prefix : STATIC_PREFIXES) {
            if (uri.startsWith(prefix)) return true;
        }
        for (String exact : STATIC_EXACT) {
            if (uri.equals(exact)) return true;
        }
        return false;
    }

    /** صفحهٔ قابلِ ثبت: نه استاتیک، نه API. */
    public static boolean isTrackablePage(String uri) {
        return !isStaticAsset(uri) && !uri.startsWith("/api/") && !uri.startsWith("/torob_api/");
    }
}
