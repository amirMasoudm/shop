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

    /**
     * 🔴 صفحه‌های داخلیِ کارکنان — رفتارِ مشتری نیستند و نباید در آمار بیایند.
     * <p>
     * این فهرست لایهٔ اولِ دفاع است؛ لایهٔ دوم و مهم‌تر در {@code UserEventRecorder}
     * است که هر کاربرِ غیرِ {@code USER} را کلاً از ثبت کنار می‌گذارد — چون کارشناسی
     * که در خودِ فروشگاه می‌گردد از همین مسیرهای عمومی رد می‌شود و فهرستِ مسیر
     * به‌تنهایی نمی‌گیردش.
     */
    private static final String[] PANEL_PREFIXES = {
            "/Admin.html", "/SalesPanel.html", "/AdminLogin.html",
            "/Financial.html", "/AnbarMali.html"
    };

    private AnalyticsPaths() {}

    public static boolean isPanelPage(String uri) {
        if (uri == null) return false;
        for (String prefix : PANEL_PREFIXES) {
            if (uri.startsWith(prefix)) return true;
        }
        return false;
    }

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

    /** صفحهٔ قابلِ ثبت: نه استاتیک، نه API، نه پنلِ کارکنان. */
    public static boolean isTrackablePage(String uri) {
        return !isStaticAsset(uri)
                && !isPanelPage(uri)
                && !uri.startsWith("/api/")
                && !uri.startsWith("/torob_api/");
    }
}
