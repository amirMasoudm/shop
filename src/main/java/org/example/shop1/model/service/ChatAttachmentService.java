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

    /** سقفِ حجمِ هر ضمیمه. با {@code spring.servlet.multipart.max-file-size} هم‌تراز است. */
    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    /**
     * لیستِ سفیدِ MIME → پسوندی که <b>ما</b> تعیین می‌کنیم.
     * <p>
     * پسوند عمداً از نامِ فایلِ کاربر گرفته نمی‌شود: کاربر می‌تواند
     * {@code x.html} یا {@code x.svg} بفرستد و اگر روزی مسیرِ ذخیره اشتباهی سرو شود،
     * همان فایل در دامنهٔ ما اجرا می‌شود. با نگاشتِ زیر، خروجی همیشه یکی از همین‌هاست.
     */
    private static final Map<String, String> ALLOWED_MIME = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp",
            "application/pdf", ".pdf",
            "text/plain", ".txt",
            "application/zip", ".zip"
    );

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
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "حجمِ فایل نباید از ۵ مگابایت بیشتر باشد");
        }

        String mime = file.getContentType() == null
                ? "" : file.getContentType().toLowerCase(Locale.ROOT).split(";")[0].trim();
        String extension = ALLOWED_MIME.get(mime);
        if (extension == null) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "این نوع فایل مجاز نیست. عکس، PDF، متن یا zip بفرستید.");
        }

        try {
            Path dir = Paths.get(chatDir);
            Files.createDirectories(dir);
            String storedName = UUID.randomUUID() + extension;
            Files.write(dir.resolve(storedName), file.getBytes());

            ChatMessage.Attachment attachment = new ChatMessage.Attachment();
            attachment.setStoredName(storedName);
            attachment.setName(safeDisplayName(file.getOriginalFilename(), extension));
            attachment.setSizeBytes(file.getSize());
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
