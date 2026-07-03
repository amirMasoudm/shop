package org.example.shop1.config;

public class SecurityUtils {
    // این متد کدهای HTML را کلاً حذف می‌کند و متن تمیز برمی‌گرداند
    public static String clean(String dirty) {
        if (dirty == null) return null;
        return org.jsoup.Jsoup.clean(dirty, String.valueOf(org.jsoup.nodes.Entities.EscapeMode.xhtml), org.jsoup.safety.Safelist.none());
    }
}