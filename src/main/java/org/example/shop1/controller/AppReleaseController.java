package org.example.shop1.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * فایل‌های انتشارِ اپِ اندروید — {@code /app/dadehlink/latest.json} و {@code /app/dadehlink/<file>.apk}.
 * مالک فایل‌ها را با FileZilla در {@code ${app.upload.dir}/app-dadehlink/} می‌گذارد.
 * <p>
 * عمداً کنترلر و نه ResourceHandler: سرآیندِ کش برای دو نوعِ فایل فرق دارد ({@code latest.json}
 * هرگز کش نشود، APKِ نام‌دار یک سال)، و فقط نامِ سادهٔ فایل پذیرفته می‌شود — نه زیرپوشه، نه
 * {@code ..}، نه فهرستِ پوشه. نبودِ فایل ۴۰۴ِ بی‌بدنه است.
 */
@Controller
public class AppReleaseController {

    static final Pattern FILE = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,120}\\.(apk|json)$");
    public static final String DIR_NAME = "app-dadehlink";
    private static final MediaType APK = MediaType.parseMediaType("application/vnd.android.package-archive");

    private final Path dir;
    private final ObjectMapper json;

    public AppReleaseController(@Value("${app.upload.dir}") String uploadDir, ObjectMapper json) {
        this.dir = Path.of(uploadDir).toAbsolutePath().normalize().resolve(DIR_NAME);
        this.json = json;
    }

    @GetMapping("/app/dadehlink/{file:.+}")
    public ResponseEntity<FileSystemResource> file(@PathVariable String file) {
        if (!FILE.matcher(file).matches()) return ResponseEntity.notFound().build();
        Path p = dir.resolve(file).normalize();
        if (!p.startsWith(dir) || !Files.isRegularFile(p)) return ResponseEntity.notFound().build();
        boolean manifest = file.endsWith(".json");
        return ResponseEntity.ok()
                .contentType(manifest ? MediaType.APPLICATION_JSON : APK)
                .cacheControl(manifest ? CacheControl.noCache() : CacheControl.maxAge(Duration.ofDays(365)).cachePublic())
                .body(new FileSystemResource(p));
    }

    /** نسخهٔ منتشرشده، فقط اگر {@code latest.json} هست و APKش واقعاً در همین پوشه است. */
    public record Release(String versionName, String path, Long sizeBytes) {}

    public Optional<Release> latest() {
        try {
            Path m = dir.resolve("latest.json");
            if (!Files.isRegularFile(m)) return Optional.empty();
            JsonNode n = json.readTree(Files.readString(m));
            String version = n.path("versionName").asText("").trim();
            String apkPath = URI.create(n.path("apkUrl").asText("")).getPath();
            if (version.isEmpty() || apkPath == null || !apkPath.startsWith("/app/dadehlink/")) return Optional.empty();
            String name = apkPath.substring("/app/dadehlink/".length());
            if (!FILE.matcher(name).matches() || !Files.isRegularFile(dir.resolve(name))) return Optional.empty();
            return Optional.of(new Release(version, apkPath, n.hasNonNull("sizeBytes") ? n.get("sizeBytes").asLong() : null));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
