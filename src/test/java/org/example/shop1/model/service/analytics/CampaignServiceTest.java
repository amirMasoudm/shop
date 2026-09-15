package org.example.shop1.model.service.analytics;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.example.shop1.exeption.ApiException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * نگهبانِ دو چیزی که شکستنشان بی‌صدا است.
 * <p>
 * اسلاگ: اگر روزی تبدیلِ فارسی به لاتین خراب شود، کارزارها اسلاگِ خالی یا تکراری
 * می‌گیرند و گزارش روی هم می‌افتد — و چون خطایی بالا نمی‌آید، تا ماه‌ها معلوم نمی‌شود.
 * <p>
 * مقصد: این همان چکی است که نمی‌گذارد لینکِ کوتاهِ ما ریدایرکتِ باز شود.
 */
class CampaignServiceTest {

    @Test
    void persianNameBecomesCleanLatinSlug() {
        assertEquals("karzar-nvrvz-1405", CampaignService.slugify("کارزارِ نوروز ۱۴۰۵"));
        assertEquals("mikrotik-2026", CampaignService.slugify("MikroTik   2026"));
        // اسلاگ هیچ‌وقت خالی نمی‌ماند، حتی اگر نام فقط نشانه‌گذاری باشد
        assertEquals("campaign", CampaignService.slugify("!!!"));
        assertFalse(CampaignService.slugify("تخفیفِ ویژهٔ پاییز").isEmpty());
        // نه فاصله، نه حرفِ غیرلاتین، نه خط‌تیرهٔ اضافه در دو سر
        String s = CampaignService.slugify("  نمایشگاهِ   تهران ۱۴۰۵!  ");
        assertTrue(s.matches("[a-z0-9]+(-[a-z0-9]+)*"), "اسلاگِ نامعتبر: " + s);
    }

    @Test
    void landingPathRejectsEverythingThatLeavesOurDomain() {
        assertEquals("/shop", CampaignService.normalizeLandingPath("/shop"));
        assertEquals("/shop?x=1", CampaignService.normalizeLandingPath(" /shop?x=1 "));
        assertEquals("/", CampaignService.normalizeLandingPath(null));

        // 🔴 هر کدام از این‌ها یک راهِ شناخته‌شدهٔ ساختنِ ریدایرکتِ باز است
        for (String bad : new String[]{
                "https://evil.example", "http://evil.example", "//evil.example",
                "/\\evil.example", "javascript:alert(1)", "/shop\nLocation: https://evil.example",
                "/l/abcdefg"}) {
            assertThrows(ApiException.class,
                    () -> CampaignService.normalizeLandingPath(bad),
                    "این مقصد باید رد می‌شد: " + bad);
        }
    }

    /** کدِ QR واقعاً باید به همان نشانی اسکن شود، نه فقط «یک تصویرِ معتبر» باشد. */
    @Test
    void qrCodeScansBackToTheShortUrl() throws Exception {
        String url = "https://dadehnama.com/l/435qww8";

        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.MARGIN, 2);
        BitMatrix matrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 512, 512, hints);

        Result decoded = new MultiFormatReader().decode(
                new BinaryBitmap(new HybridBinarizer(new MatrixLuminanceSource(matrix))));
        assertEquals(url, decoded.getText());
    }

    /**
     * منبعِ روشناییِ کمینه از {@code BitMatrix}.
     * <p>
     * {@code BufferedImageLuminanceSource} در ماژولِ {@code javase} است و آن را
     * فقط برای همین تست به وابستگی‌ها اضافه نمی‌کنیم.
     */
    private static final class MatrixLuminanceSource extends LuminanceSource {
        private final BitMatrix matrix;

        MatrixLuminanceSource(BitMatrix matrix) {
            super(matrix.getWidth(), matrix.getHeight());
            this.matrix = matrix;
        }

        @Override
        public byte[] getRow(int y, byte[] row) {
            if (row == null || row.length < getWidth()) row = new byte[getWidth()];
            for (int x = 0; x < getWidth(); x++) {
                row[x] = (byte) (matrix.get(x, y) ? 0 : 255);
            }
            return row;
        }

        @Override
        public byte[] getMatrix() {
            byte[] out = new byte[getWidth() * getHeight()];
            for (int y = 0; y < getHeight(); y++) {
                for (int x = 0; x < getWidth(); x++) {
                    out[y * getWidth() + x] = (byte) (matrix.get(x, y) ? 0 : 255);
                }
            }
            return out;
        }
    }
}
