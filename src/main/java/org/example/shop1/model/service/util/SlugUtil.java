package org.example.shop1.model.service.util;

/**
 * منطق مشترک ساخت اسلاگ URL برای محصول و دسته‌بندی.
 * حروف/ارقام فارسی حفظ می‌شوند؛ نقطه‌گذاری اَسکی (به‌جز خط تیره) حذف می‌شود.
 */
public final class SlugUtil {

    private SlugUtil() {}

    public static String slugify(String s) {
        if (s == null) return "";
        String out = s.trim().replaceAll("\\s+", "-");
        // حذف نقطه‌گذاری اَسکی به‌جز خط تیره (شامل / ? # % & . , و ...)
        out = out.replaceAll("[\\p{Punct}&&[^-]]", "");
        // جمع‌کردن خط‌تیره‌های پشت‌سرهم و حذف از ابتدا/انتها
        out = out.replaceAll("-{2,}", "-").replaceAll("^-+|-+$", "");
        return out;
    }
}
