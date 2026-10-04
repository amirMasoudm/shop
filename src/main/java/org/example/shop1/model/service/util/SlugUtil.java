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

    /**
     * قانونِ سختِ دُمِ فارسیِ آدرسِ هیبریدِ محصول:
     * فقط حروف (فارسی/لاتین) + رقم (لاتین/فارسی) + خط‌تیره باقی می‌ماند.
     * فاصله و نیم‌فاصله → خط‌تیره؛ کاراکترهایی مثل ³ + # & / حذف می‌شوند.
     * (برخلاف slugify که فقط نقطه‌گذاریِ اَسکی را حذف می‌کند و ³ را نگه می‌داشت.)
     */
    public static String sanitizeTail(String s) {
        if (s == null) return "";
        // فاصله و نیم‌فاصله (U+200C) به خط‌تیره
        String out = s.trim().replaceAll("[\\s\\u200c]+", "-");
        // هر چیزی جز حرف/رقمِ لاتین/رقمِ فارسی/خط‌تیره حذف شود (³ حذف می‌شود چون حرف/رقم نیست)
        out = out.replaceAll("[^\\p{IsAlphabetic}0-9\\u06F0-\\u06F9-]", "");
        out = out.replaceAll("-{2,}", "-").replaceAll("^-+|-+$", "");
        return out;
    }
}
