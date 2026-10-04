package org.example.shop1.controller;

import org.example.shop1.model.entity.ActivityLog;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.entity.RfSpec;
import org.example.shop1.model.reposritory.ProductRepository;
import org.example.shop1.model.service.ActivityLogService;
import org.example.shop1.model.service.RfSpecImportService;
import org.example.shop1.model.service.RfSpecService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * دادهٔ فنیِ رادیویی از پنل — فقط ADMIN ({@code SecurityConfig}).
 * <ul>
 *   <li>{@code PUT/DELETE /{productId}}: بخشِ «مشخصاتِ رادیویی» در مودالِ ویرایشِ محصول.</li>
 *   <li>{@code import/preview} و {@code import/apply}: فایل‌های چت ب، با پیش‌نمایش.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/rf-specs")
public class RfSpecAdminController {

    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;

    private final RfSpecService rf;
    private final RfSpecImportService importer;
    private final ProductRepository products;
    private final ActivityLogService activityLog;

    public RfSpecAdminController(RfSpecService rf, RfSpecImportService importer, ProductRepository products,
                                 ActivityLogService activityLog) {
        this.rf = rf;
        this.importer = importer;
        this.products = products;
        this.activityLog = activityLog;
    }

    @PutMapping("/{productId}")
    public ResponseEntity<?> update(@PathVariable String productId, @RequestBody RfSpec body) {
        Product p = rf.updateManual(productId, body);
        return ResponseEntity.ok(Map.of("rf", p.getRf(), "rfUpdatedAt", String.valueOf(p.getRfUpdatedAt())));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<?> clear(@PathVariable String productId) {
        Product p = products.findById(productId).orElseThrow(() -> new IllegalArgumentException("محصول پیدا نشد"));
        if (p.getRf() != null) {
            String before = RfSpecService.summary(p.getRf());
            p.setRf(null);
            p.setRfUpdatedAt(Instant.now());
            products.save(p);
            activityLog.recordProduct(ActivityLog.Action.PRODUCT_UPDATE, ActivityLog.Source.MANUAL,
                    p.getId(), p.getName(), "rf", before, "—");
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import/preview")
    public ResponseEntity<?> preview(@RequestParam("products") MultipartFile productsFile,
                                     @RequestParam(value = "rates", required = false) MultipartFile ratesFile) throws Exception {
        if (productsFile.isEmpty() || productsFile.getSize() > MAX_FILE_BYTES
                || (ratesFile != null && ratesFile.getSize() > MAX_FILE_BYTES)) {
            throw new IllegalArgumentException("فایل خالی است یا از ۵ مگابایت بزرگ‌تر");
        }
        String rates = ratesFile == null || ratesFile.isEmpty() ? null
                : new String(ratesFile.getBytes(), StandardCharsets.UTF_8);
        RfSpecImportService.Preview pv = importer.preview(new String(productsFile.getBytes(), StandardCharsets.UTF_8),
                productsFile.getOriginalFilename(), rates, ratesFile == null ? null : ratesFile.getOriginalFilename());
        // specs عمداً بیرون نمی‌رود: پیش‌نمایش فقط «چه خانه‌ای چه می‌شود» را لازم دارد.
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("token", pv.token());
        out.put("fileNames", pv.fileNames());
        out.put("rowsRead", pv.rowsRead());
        out.put("rateRowsRead", pv.rateRowsRead());
        out.put("counts", pv.counts());
        out.put("items", pv.items());
        return ResponseEntity.ok(out);
    }

    @PostMapping("/import/apply")
    public ResponseEntity<?> apply(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(importer.apply(body.get("token")));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> bad(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}
