package org.example.shop1.model.service;

import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.ChatMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * ذخیره و خواندنِ ضمیمه‌هایِ چت.
 * <p>
 * 🔴 <b>مهم‌ترین قیدِ امنیتیِ این فیچر:</b> این فایل‌ها هرگز نباید در
 * {@code /opt/shop/uploads/} بنشینند. آن مسیر عمومی سرو می‌شود (و باید هم بشود —
 * عکسِ محصول است)، ولی ضمیمهٔ چت فایلِ خصوصیِ یک مشتریِ مشخص است. مسیرِ اینجا
 * ({@code app.chat.dir}) با هیچ {@code ResourceHandler} یا {@code location}ِ nginx
 * سرو نمی‌شود؛ تنها راهِ گرفتنش کنترلری است که عضویتِ درخواست‌کننده را چک می‌کند.
 * <p>
 * ⚠️ نامِ UUID «امنیت» نیست، «ابهام» است — به آن تکیه نشده؛ چکِ عضویت کارِ اصلی را
 * می‌کند. یادآوری: ۳۰ اوت یک {@code .env} واقعی از مسیرِ آپلودِ عمومی سرو می‌شد.
 */
@Service
public class ChatAttachmentService {

    /**
     * سقفِ حجمِ ضمیمهٔ <b>غیرتصویری</b>.
     * <p>
     * عکس‌ها این سقف را ندارند چون قبل از ذخیره فشرده می‌شوند (نگاه کن به {@link #store}):
     * عکسِ ۸ مگابایتیِ گوشی بعد از کوچک‌شدن چند صد کیلوبایت می‌شود. اگر اینجا هم رد
     * می‌شد، معمول‌ترین کاربردِ ضمیمه در پشتیبانیِ تجهیزات — «عکسِ لیبل/دستگاه را
     * بفرست» — عملاً کار نمی‌کرد.
     */
    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    /** سقفِ عکس بعد از فشرده‌سازی؛ اگر باز هم بزرگ‌تر بود یعنی واقعاً غیرعادی است. */
    private static final long MAX_IMAGE_STORED_BYTES = 8L * 1024 * 1024;

    /**
     * لیستِ سفیدِ MIME → پسوندی که <b>ما</b> تعیین می‌کنیم.
     * <p>
     * پسوند عمداً از نامِ فایلِ کاربر گرفته نمی‌شود: کاربر می‌تواند
     * {@code x.html} یا {@code x.svg} بفرستد و اگر روزی مسیرِ ذخیره اشتباهی سرو شود،
     * همان فایل در دامنهٔ ما اجرا می‌شود. با نگاشتِ زیر، خروجی همیشه یکی از همین‌هاست.
     * <p>
     * فرمت‌های اداری و آرشیو عمداً هستند: مشتریِ تجهیزاتِ شبکه پیش‌فاکتور و لیستِ
     * قطعات را با همین‌ها می‌فرستد. خطرشان مهار شده — هیچ‌کدام تصویر نیستند، پس با
     * {@code Content-Disposition: attachment} و {@code nosniff} تحویل می‌شوند و در
     * دامنهٔ ما اجرا نمی‌شوند.
     */
    private static final Map<String, String> ALLOWED_MIME = Map.ofEntries(
            Map.entry("image/jpeg", ".jpg"),
            Map.entry("image/png", ".png"),
            Map.entry("image/gif", ".gif"),
            Map.entry("image/webp", ".webp"),
            Map.entry("image/heic", ".heic"),
            Map.entry("application/pdf", ".pdf"),
            Map.entry("text/plain", ".txt"),
            Map.entry("text/csv", ".csv"),
            Map.entry("application/zip", ".zip"),
            Map.entry("application/x-zip-compressed", ".zip"),
            Map.entry("application/x-rar-compressed", ".rar"),
            Map.entry("application/vnd.rar", ".rar"),
            Map.entry("application/x-7z-compressed", ".7z"),
            Map.entry("application/msword", ".doc"),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
            Map.entry("application/vnd.ms-excel", ".xls"),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx")
    );

    /** برایِ صفتِ {@code accept} در فرم — تا پیکرِ مرورگر همان اول فیلتر کند. */
    public static String allowedMimeList() {
        return String.join(",", ALLOWED_MIME.keySet());
    }

    private final FileStorageService fileStorageService;

    public ChatAttachmentService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Value("${app.chat.dir}")
    private String chatDir;

    public static boolean isImage(String mime) {
        return mime != null && mime.startsWith("image/");
    }

    /** ذخیره روی دیسک و برگرداندنِ توصیفِ ضمیمه برایِ نشستن در سندِ پیام. */
    public ChatMessage.Attachment store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "فایلی انتخاب نشده است");
        }

        String mime = file.getContentType() == null
                ? "" : file.getContentType().toLowerCase(Locale.ROOT).split(";")[0].trim();
        String extension = ALLOWED_MIME.get(mime);
        if (extension == null) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "این نوع فایل مجاز نیست. عکس، PDF، فایلِ آفیس، متن یا فایلِ فشرده بفرستید.");
        }
        boolean image = isImage(mime);
        if (!image && file.getSize() > MAX_SIZE_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "حجمِ فایل نباید از ۵ مگابایت بیشتر باشد");
        }

        try {
            byte[] bytes = file.getBytes();

            // عکس قبل از ذخیره کوچک و دوباره فشرده می‌شود — همان مسیری که عکسِ محصول
            // از آن رد می‌شود. بدونِ این، عکسِ معمولیِ گوشی رد می‌شد.
            if (image) {
                FileStorageService.Optimized optimized =
                        fileStorageService.optimize(bytes, ".png".equals(extension));
                if (optimized != null) {
                    bytes = optimized.bytes();
                    extension = optimized.extension();
                    mime = ".png".equals(extension) ? "image/png" : "image/jpeg";
                }
                if (bytes.length > MAX_IMAGE_STORED_BYTES) {
                    throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,
                            "این عکس حتی بعد از فشرده‌سازی هم خیلی بزرگ است");
                }
            }

            Path dir = Paths.get(chatDir);
            Files.createDirectories(dir);
            String storedName = UUID.randomUUID() + extension;
            Files.write(dir.resolve(storedName), bytes);

            ChatMessage.Attachment attachment = new ChatMessage.Attachment();
            attachment.setStoredName(storedName);
            attachment.setName(safeDisplayName(file.getOriginalFilename(), extension));
            attachment.setSizeBytes(bytes.length);
            attachment.setMime(mime);
            return attachment;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "ذخیرهٔ فایل ناموفق بود");
        }
    }

    public byte[] read(ChatMessage.Attachment attachment) {
        try {
            Path path = resolveInsideChatDir(attachment.getStoredName());
            if (!Files.exists(path)) {
                throw new ApiException(HttpStatus.NOT_FOUND, "فایل پیدا نشد");
            }
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "خواندنِ فایل ناموفق بود");
        }
    }

    /**
     * محافظت در برابرِ path traversal. نامِ ذخیره‌شده را خودمان ساخته‌ایم، ولی این
     * چک اینجاست تا اگر روزی سندِ پیام از مسیرِ دیگری پر شد (ایمپورت، اصلاحِ دستیِ
     * دیتابیس) نتواند به بیرونِ پوشهٔ چت اشاره کند.
     */
    private Path resolveInsideChatDir(String storedName) throws IOException {
        Path base = Paths.get(chatDir).toAbsolutePath().normalize();
        Path resolved = base.resolve(storedName).normalize();
        if (!resolved.startsWith(base)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "مسیرِ فایل نامعتبر است");
        }
        return resolved;
    }

    /** نامِ نمایشی فقط برای کاربر است؛ مسیرِ ذخیره از آن ساخته نمی‌شود. */
    private String safeDisplayName(String original, String extension) {
        if (original == null || original.isBlank()) return "attachment" + extension;
        String base = Paths.get(original).getFileName().toString();
        return base.length() > 120 ? base.substring(0, 120) : base;
    }
}
