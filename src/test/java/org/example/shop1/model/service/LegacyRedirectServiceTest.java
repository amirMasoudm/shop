package org.example.shop1.model.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * نگهبانِ دو چیزی که هر دو <b>بی‌صدا</b> می‌شکنند.
 * <p>
 * نرمال‌سازی: اگر خراب شود، ریدایرکت‌ها فقط «پیدا نمی‌شوند» — نه خطایی، نه لاگی،
 * فقط ۴۰۴. و هدرِ {@code Location}: اگر نویسهٔ غیرِ لاتین-۱ داشته باشد، تامکت کلِ
 * هدر را حذف می‌کند و پاسخ ۳۰۱ٍ بدونِ مقصد می‌شود. هر دو را در تستِ میدانی دیدم.
 */
class LegacyRedirectServiceTest {

    @Test
    void allFourShapesOfTheSameUrlProduceOneKey() {
        String expected = "/عیبیابی-میکروتیک";
        // همان‌طور که وردپرس ذخیره کرده — درصد-کدشده با حروفِ کوچک
        assertEquals(expected, LegacyRedirectService.normalize(
                "/%d8%b9%db%8c%d8%a8%db%8c%d8%a7%d8%a8%db%8c-%d9%85%db%8c%da%a9%d8%b1%d9%88%d8%aa%db%8c%da%a9/"));
        // همان‌طور که مرورگر می‌فرستد — حروفِ بزرگ
        assertEquals(expected, LegacyRedirectService.normalize(
                "/%D8%B9%DB%8C%D8%A8%DB%8C%D8%A7%D8%A8%DB%8C-%D9%85%DB%8C%DA%A9%D8%B1%D9%88%D8%AA%DB%8C%DA%A9"));
        // با دنبالهٔ کارزار
        assertEquals(expected, LegacyRedirectService.normalize(
                "/عیبیابی-میکروتیک/?utm_source=telegram&utm_medium=social"));
        // و به‌صورتِ نشانیِ کامل، آن‌طور که در فایلِ نگاشت هست
        assertEquals(expected, LegacyRedirectService.normalize(
                "https://dadehnama.com/عیبیابی-میکروتیک/"));
    }

    @Test
    void brokenPercentEncodingDoesNotThrow() {
        // 🔴 این مسیر از بیرون می‌آید؛ استثنا یعنی ۵۰۰ روی صفحه‌ای که باید ۴۰۴ باشد
        assertDoesNotThrow(() -> LegacyRedirectService.normalize("/%ZZ%ZZ-chiz"));
        assertDoesNotThrow(() -> LegacyRedirectService.normalize("/%"));
        assertDoesNotThrow(() -> LegacyRedirectService.normalize("/%e0%a4%a"));
        assertNotNull(LegacyRedirectService.normalize(null));
    }

    @Test
    void appPathsCanNeverBecomeRedirectSources() {
        for (String reserved : new String[]{
                "/", "/shop", "/shop/product/x", "/blog", "/blog/anything",
                "/api/v1/products", "/l/abc123", "/admin.html", "/uploads/x.jpg", "/js/app.js"}) {
            assertTrue(LegacyRedirectService.isReservedPath(LegacyRedirectService.normalize(reserved)),
                    "این مسیر باید رزرو باشد: " + reserved);
        }
        // ولی یک اسلاگِ ریشه‌ایِ وردپرس آزاد است
        assertFalse(LegacyRedirectService.isReservedPath(
                LegacyRedirectService.normalize("/عیبیابی-میکروتیک/")));
    }

    @Test
    void locationHeaderKeepsLeadingSlashAndEscapesPersian() {
        String out = LegacyRedirectService.toLocationHeader("/blog/عیبیابی-میکروتیک");
        // ⚠️ بدونِ اسلشِ ابتدایی، مرورگر مقصد را نسبی می‌گیرد و به مسیرِ بی‌معنی می‌رود
        assertTrue(out.startsWith("/blog/"), "اسلشِ ابتدایی گم شد: " + out);
        // هدرِ HTTP نویسهٔ غیرِ لاتین-۱ نمی‌پذیرد؛ تامکت بی‌صدا حذفش می‌کند
        assertTrue(out.chars().allMatch(c -> c < 128), "نویسهٔ غیر-ASCII در هدر: " + out);
        assertEquals("/learn", LegacyRedirectService.toLocationHeader("/learn"));
        assertEquals("/blog/a%20b", LegacyRedirectService.toLocationHeader("/blog/a b"));
    }
}
