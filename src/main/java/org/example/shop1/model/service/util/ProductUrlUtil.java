package org.example.shop1.model.service.util;

import org.example.shop1.model.entity.Product;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/**
 * ساختِ آدرسِ هیبریدِ محصول ({@code /product/{resolver}/{persianTail}}).
 * <p>
 * قبلاً این منطق فقط داخلِ {@code StoreWebController} و private بود؛ چون Torob API هم
 * باید <b>دقیقاً همان</b> آدرسِ canonical را بفرستد (نه یک آدرسِ موازی)، اینجا مشترک شد
 * تا یک منبعِ حقیقت داشته باشیم و دو مسیر از هم واگرا نشوند.
 */
public final class ProductUrlUtil {

    private ProductUrlUtil() {}

    /** resolverِ پایدار: اسلاگِ لاتین اگر باشد، وگرنه شناسه. */
    public static String productResolver(Product p) {
        return (p.getSlug() != null && !p.getSlug().isEmpty()) ? p.getSlug() : p.getId();
    }

    /** مسیرِ خام و خوانا (بدونِ baseUrl) — برای canonical/og در HTML.
     * 🔴 پیشوندِ {@code /shop}: کلِ زیردرختِ فروشگاه زیرِ /shop جمع شده (تصمیمِ معماریِ
     * تقسیمِ سایت به داده‌نما/فروشگاه/آموزش). */
    public static String hybridPath(Product p) {
        String resolver = productResolver(p);
        String tail = p.getPersianTail();
        return (tail != null && !tail.isEmpty()) ? "/shop/product/" + resolver + "/" + tail : "/shop/product/" + resolver;
    }

    /**
     * نسخهٔ percent-encode شده — برای هدرِ Location، sitemap و Torob API که باید ASCII باشند.
     * دُمِ فارسی و کاراکترهایی مثلِ {@code +} در نامِ مدل‌ها اینجا درست encode می‌شوند.
     */
    public static String hybridPathEncoded(Product p) {
        String resolver = UriUtils.encodePathSegment(productResolver(p), StandardCharsets.UTF_8);
        String tail = p.getPersianTail();
        if (tail == null || tail.isEmpty()) return "/shop/product/" + resolver;
        String encTail = UriUtils.encodePathSegment(tail, StandardCharsets.UTF_8);
        return "/shop/product/" + resolver + "/" + encTail;
    }
}
