package org.example.shop1.config;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public class SecurityUtils {
    /**
     * این متد تمام تگ‌های HTML را از متن حذف می‌کند.
     * اگر متن شامل تگی مثل <script> باشد، آن را کلاً حذف می‌کند.
     */
    public static String clean(String dirty) {
        if (dirty == null || dirty.trim().isEmpty()) {
            return dirty;
        }
        // استفاده از Safelist.none() یعنی هیچ تگ HTML اجازه عبور ندارد
        return Jsoup.clean(dirty, Safelist.none());
    }
}