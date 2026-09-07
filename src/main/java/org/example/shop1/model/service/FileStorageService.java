package org.example.shop1.model.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileStorageService {

    /** بیشترین عرضِ مجاز؛ عکسِ عریض‌تر کوچک می‌شود (صفحهٔ محصول به بیش از این نیاز ندارد). */
    private static final int MAX_WIDTH = 2000;

    /** کیفیتِ انکودِ JPEG؛ ۰.۸۲ تعادلِ متعارفِ حجم و کیفیت است. */
    private static final float JPEG_QUALITY = 0.82f;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public String storeFile(MultipartFile file) {
        try {
            return storeFile(file.getBytes(), file.getOriginalFilename());
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file on disk", e);
        }
    }

    /**
     * مسیرِ مشترکِ ذخیره‌سازی — هم از آپلودِ فرم (MultipartFile بالا) هم از دانلودِ
     * مستقیمِ بایت (مثلاً ایمپورتِ تصاویرِ وردپرس) استفاده می‌شود.
     */
    public String storeFile(byte[] bytes, String originalFilename) {
        try {
            // ساخت پوشه اگر وجود ندارد
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";

            // فقط JPEG و PNG بهینه می‌شوند؛ SVG و GIF و بقیه دست‌نخورده ذخیره می‌شوند
            String ext = extension.toLowerCase(Locale.ROOT);
            if (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png")) {
                Optimized optimized = optimize(bytes, ext.equals(".png"));
                if (optimized != null) {
                    bytes = optimized.bytes;
                    extension = optimized.extension;
                }
            }

            // ساخت اسم یونیک برای فایل
            String newFilename = UUID.randomUUID().toString() + extension;

            // مسیر نهایی
            Path filepath = Paths.get(uploadDir, newFilename);

            // نوشتن فایل روی هارد
            Files.write(filepath, bytes);

            return newFilename;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file on disk", e);
        }
    }

    /** نتیجهٔ بهینه‌سازی؛ چون فرمت ممکن است از PNG به JPEG تغییر کند، پسوند هم برمی‌گردد. */
    public record Optimized(byte[] bytes, String extension) {
    }

    /**
     * عکس را در صورت نیاز کوچک و دوباره فشرده می‌کند.
     * اگر فایل قابل خواندن نباشد یا نتیجه از اصل بهتر نشود، null برمی‌گرداند تا فایلِ اصلی ذخیره شود.
     * <p>
     * عمداً public است: ضمیمهٔ عکسِ چت هم از همین مسیر رد می‌شود تا عکسِ چند مگابایتیِ
     * گوشی قبل از ذخیره کوچک شود. دو پیاده‌سازیِ موازیِ فشرده‌سازی نمی‌خواهیم.
     */
    public Optimized optimize(byte[] original, boolean isPng) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(original));
            if (image == null) {
                // فایل خرابی است یا فرمتش را نمی‌شناسیم؛ دست نمی‌زنیم
                return null;
            }

            boolean needsResize = image.getWidth() > MAX_WIDTH;
            boolean hasAlpha = image.getColorModel().hasAlpha();

            // PNGِ شفاف باید PNG بماند وگرنه شفافیتش را از دست می‌دهد (لوگو و آیکون)
            if (isPng && hasAlpha) {
                if (!needsResize) {
                    return null;
                }
                BufferedImage resized = resize(image, MAX_WIDTH);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(resized, "png", out);
                return new Optimized(out.toByteArray(), ".png");
            }

            BufferedImage target = needsResize ? resize(image, MAX_WIDTH) : image;
            byte[] encoded = encodeJpeg(flattenToRgb(target));

            // اگر عکس از قبل بهینه بوده و انکودِ دوباره چیزی کم نکرده، اصل را نگه می‌داریم
            if (!needsResize && encoded.length >= original.length) {
                return null;
            }
            return new Optimized(encoded, ".jpg");

        } catch (IOException e) {
            // بهینه‌سازی نباید جلوی آپلود را بگیرد؛ فایلِ اصلی ذخیره می‌شود
            return null;
        }
    }

    /** کوچک‌کردن با حفظ نسبتِ ابعاد. */
    private BufferedImage resize(BufferedImage source, int targetWidth) {
        int targetHeight = Math.max(1, Math.round(
                source.getHeight() * (targetWidth / (float) source.getWidth())));

        int type = source.getColorModel().hasAlpha()
                ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;

        BufferedImage result = new BufferedImage(targetWidth, targetHeight, type);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return result;
    }

    /** JPEG کانالِ شفافیت ندارد؛ پس‌زمینهٔ شفاف سفید می‌شود. */
    private BufferedImage flattenToRgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }
        BufferedImage rgb = new BufferedImage(
                source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, source.getWidth(), source.getHeight());
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return rgb;
    }

    private byte[] encodeJpeg(BufferedImage image) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer available");
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(JPEG_QUALITY);
            }
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }
}
