package org.example.shop1.controller;

import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.HolooCode;
import org.example.shop1.model.service.HolooCodeService;
import org.example.shop1.model.service.holoo.HolooStockImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * کدِ کالا و همگام‌سازیِ موجودی.
 * <p>
 * 🔴 همهٔ مسیرها زیرِ {@code /api/v1/holoo/**} هستند و در {@code SecurityConfig} فقط
 * به ADMIN و PRICER باز می‌شوند. کدِ کالا در پاسخِ عمومیِ محصول <b>دیده می‌شود</b>
 * (تصمیمِ عمدیِ مالک) ولی هیچ مسیرِ عمومی‌ای نمی‌تواند آن را بسازد یا عوض کند؛ این
 * کنترلر تنها راهِ نوشتن است.
 */
@RestController
@RequestMapping("/api/v1/holoo")
public class HolooController {

    /** سقفِ اندازهٔ فایل — خروجیِ موجودی چند ده کیلوبایت است؛ این سقف با فاصلهٔ زیاد بالاتر است. */
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    private final HolooCodeService codeService;
    private final HolooStockImportService importService;

    public HolooController(HolooCodeService codeService, HolooStockImportService importService) {
        this.codeService = codeService;
        this.importService = importService;
    }

    // ==========================================================
    // کد
    // ==========================================================

    /** «دریافتِ کدِ هلو» — جریانِ کد‌اول. */
    @PostMapping("/codes/reserve")
    public ResponseEntity<HolooCode> reserve(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(codeService.reserve(body == null ? null : body.get("note")));
    }

    /** رزروهایِ بی‌صاحب — «کد گرفتم و یادم رفت» اینجا دیده می‌شود. */
    @GetMapping("/codes/reserved")
    public ResponseEntity<List<HolooCode>> reserved() {
        return ResponseEntity.ok(codeService.openReservations());
    }

    /**
     * صدورِ کد برایِ محصولی که هنوز کد ندارد.
     * <p>
     * 🔴 عمداً اندپوینتِ جداست و فیلدی در {@code PUT /api/v1/products/{id}} نشد.
     * اگر در مسیرِ ذخیره می‌نشست، هر ویرایشِ محصول یک شماره می‌سوزاند — همان قاعده‌ای
     * که تسکِ {@code updateproduct-fix} سابقه‌اش را دارد. صدور باید عملِ صریح باشد.
     */
    @PostMapping("/codes/assign")
    public ResponseEntity<Map<String, Object>> assign(@RequestBody Map<String, String> body) {
        String productId = body == null ? null : body.get("productId");
        if (productId == null || productId.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "شناسهٔ محصول نیامده است");
        }
        String code = codeService.assignToExistingProduct(productId, body.get("code"));
        return ResponseEntity.ok(Map.of("productId", productId, "holooCode", code));
    }

    /** وضعیتِ شمارنده و قالبِ کد — برایِ گزارش و ممیزی. */
    @GetMapping("/codes/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("counter", codeService.currentSequence());
        m.put("prefix", codeService.prefix());
        m.put("digits", codeService.digits());
        m.put("nextWouldBe", codeService.format(codeService.currentSequence() + 1));
        m.put("openReservations", codeService.openReservations().size());
        return ResponseEntity.ok(m);
    }

    /** مهاجرتِ یک‌باره از CSVِ توافق‌شده. اجرای دوباره بی‌اثر است. */
    @PostMapping(value = "/codes/migrate", consumes = {"text/csv", MediaType.TEXT_PLAIN_VALUE})
    public ResponseEntity<HolooCodeService.MigrationResult> migrate(@RequestBody String csv) {
        return ResponseEntity.ok(codeService.migrateFromCsv(csv));
    }

    // ==========================================================
    // ایمپورتِ موجودی
    // ==========================================================

    /**
     * مرحلهٔ ۱ — پیش‌نمایش. هیچ‌چیز نوشته نمی‌شود.
     * <p>
     * چند فایل هم‌زمان پذیرفته می‌شود چون خروجیِ واقعی دو فایل است (یکی به‌ازای هر
     * انبار). اگر جدا آپلود شوند، هر ایمپورت فقط شعبهٔ خودش را پوشش می‌دهد و شعبهٔ
     * دیگر دست‌نخورده می‌ماند — که درست است ولی دو تأیید می‌خواهد.
     */
    @PostMapping(value = "/stock/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HolooStockImportService.Preview> preview(@RequestParam("files") MultipartFile[] files) {
        return ResponseEntity.ok(importService.preview(toUploads(files)));
    }

    /** مرحلهٔ ۲ — اعمال، با تأییدِ جدا. */
    @PostMapping("/stock/apply")
    public ResponseEntity<HolooStockImportService.ApplyResult> apply(@RequestBody Map<String, Object> body) {
        String token = body == null ? null : String.valueOf(body.get("token"));
        if (token == null || token.isBlank() || "null".equals(token)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "شناسهٔ پیش‌نمایش نیامده است");
        }
        boolean sendSms = Boolean.TRUE.equals(body.get("sendSms"));
        boolean confirmMassZero = Boolean.TRUE.equals(body.get("confirmMassZero"));
        return ResponseEntity.ok(importService.apply(token, sendSms, confirmMassZero));
    }

    private List<HolooStockImportService.UploadedFile> toUploads(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "فایلی انتخاب نشده است");
        }
        List<HolooStockImportService.UploadedFile> out = new ArrayList<>();
        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) continue;
            if (f.getSize() > MAX_FILE_BYTES) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "فایلِ «" + f.getOriginalFilename() + "» بیش از حد بزرگ است");
            }
            try {
                out.add(new HolooStockImportService.UploadedFile(f.getOriginalFilename(), f.getBytes()));
            } catch (IOException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "خواندنِ فایلِ «" + f.getOriginalFilename() + "» ناموفق بود");
            }
        }
        if (out.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "فایلی انتخاب نشده است");
        }
        return out;
    }
}
