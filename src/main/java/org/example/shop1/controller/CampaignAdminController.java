package org.example.shop1.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import jakarta.servlet.http.HttpServletRequest;
import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.ActivityLog;
import org.example.shop1.model.entity.Campaign;
import org.example.shop1.model.reposritory.CampaignRepository;
import org.example.shop1.model.service.ActivityLogService;
import org.example.shop1.model.service.analytics.CampaignService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * دفترِ کارزارها و لینک‌ساز.
 * <p>
 * 🔴 کلِ این مسیر در {@code SecurityConfig} فقط {@code ADMIN} است: هزینهٔ کارزار و
 * برنامهٔ بازاریابی اینجاست.
 */
@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignAdminController {

    /** کوچک‌تر از این، اسکنِ QR از روی کاغذِ چاپی سخت می‌شود. */
    private static final int QR_MIN_PX = 128;
    private static final int QR_MAX_PX = 2048;
    private static final int QR_DEFAULT_PX = 512;

    private final CampaignRepository repo;
    private final CampaignService service;
    private final ActivityLogService activityLog;

    public CampaignAdminController(CampaignRepository repo, CampaignService service,
                                   ActivityLogService activityLog) {
        this.repo = repo;
        this.service = service;
        this.activityLog = activityLog;
    }

    /** واژگانِ مجازِ {@code source} و {@code medium} — پنل دراپ‌داون را از همین می‌سازد. */
    @GetMapping("/vocabularies")
    public ResponseEntity<Map<String, List<String>>> vocabularies() {
        return ResponseEntity.ok(service.vocabularies());
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(HttpServletRequest request) {
        String baseUrl = CampaignService.baseUrlOf(request);
        List<Map<String, Object>> out = repo.findAllByOrderByCreatedAtDesc().stream()
                .map(c -> withLinks(c, baseUrl)).toList();
        return ResponseEntity.ok(out);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Campaign body, HttpServletRequest request) {
        Campaign saved = service.create(body);
        log(saved, "ساختِ کارزار", null, saved.getSlug());
        return ResponseEntity.ok(withLinks(saved, CampaignService.baseUrlOf(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable String id, @RequestBody Campaign body,
                                                     HttpServletRequest request) {
        Campaign before = repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "کارزار پیدا نشد"));
        boolean wasActive = before.isActive();
        Campaign saved = service.update(id, body);
        log(saved, "ویرایشِ کارزار", String.valueOf(wasActive), String.valueOf(saved.isActive()));
        return ResponseEntity.ok(withLinks(saved, CampaignService.baseUrlOf(request)));
    }

    /**
     * کدِ QRِ لینکِ کوتاه به‌صورتِ PNG.
     * <p>
     * تصحیحِ خطا روی {@code M} است: حدودِ ۱۵٪ خرابی را تحمل می‌کند، که برایِ کاغذِ
     * چاپی و غرفهٔ نمایشگاه — جایی که کد خط می‌افتد و کج اسکن می‌شود — لازم است،
     * بدونِ اینکه تصویر را بی‌جهت شلوغ کند.
     */
    @GetMapping(value = "/{id}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(@PathVariable String id,
                                     @RequestParam(defaultValue = "" + QR_DEFAULT_PX) int size,
                                     HttpServletRequest request) throws Exception {
        Campaign c = repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "کارزار پیدا نشد"));
        int px = Math.max(QR_MIN_PX, Math.min(QR_MAX_PX, size));

        String shortUrl = service.shortUrl(c, CampaignService.baseUrlOf(request));

        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.MARGIN, 2);

        BitMatrix matrix = new QRCodeWriter().encode(shortUrl, BarcodeFormat.QR_CODE, px, px, hints);
        BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < matrix.getWidth(); x++) {
            for (int y = 0; y < matrix.getHeight(); y++) {
                image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"qr-" + c.getCode() + ".png\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, private")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.IMAGE_PNG)
                .body(out.toByteArray());
    }

    /**
     * سندِ کارزار به‌علاوهٔ هر دو لینک.
     * <p>
     * لینک‌ها در پاسخ ساخته می‌شوند، نه در دیتابیس: دامنه ممکن است عوض شود (همین حالا
     * لوکال و سرور دو دامنهٔ متفاوت‌اند) و لینکِ ذخیره‌شده همان‌جا کهنه می‌شد.
     */
    private Map<String, Object> withLinks(Campaign c, String baseUrl) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("name", c.getName());
        m.put("slug", c.getSlug());
        m.put("code", c.getCode());
        m.put("source", c.getSource());
        m.put("medium", c.getMedium());
        m.put("term", c.getTerm());
        m.put("content", c.getContent());
        m.put("landingPath", c.getLandingPath());
        m.put("startsAt", c.getStartsAt());
        m.put("endsAt", c.getEndsAt());
        m.put("cost", c.getCost());
        m.put("active", c.isActive());
        m.put("notes", c.getNotes());
        m.put("createdAt", c.getCreatedAt());
        m.put("taggedUrl", service.taggedUrl(c, baseUrl));
        m.put("shortUrl", service.shortUrl(c, baseUrl));
        m.put("qrUrl", "/api/v1/campaigns/" + c.getId() + "/qr.png");
        return m;
    }

    private void log(Campaign c, String title, String oldValue, String newValue) {
        activityLog.record(ActivityLog.Action.ANALYTICS_EXPORT, ActivityLog.Source.MANUAL,
                ActivityLogService.ENTITY_ANALYTICS, c.getId(), title,
                "campaign:" + c.getSlug(), oldValue, newValue);
    }
}
