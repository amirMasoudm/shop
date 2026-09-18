package org.example.shop1.controller;

import org.example.shop1.model.dto.PricingRowDto;
import org.example.shop1.model.entity.ActivityLog;
import org.example.shop1.model.service.ActivityLogService;
import org.example.shop1.model.service.PricingWorkspaceService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * میزِ کارِ قیمت‌گذاریِ داخلی.
 * <p>
 * دسترسی در {@code SecurityConfig} کنترل می‌شود:
 * <ul>
 *   <li>{@code GET /api/v1/pricing/rows} → ADMIN، PRICER، SALES (خواندن)</li>
 *   <li>{@code POST /api/v1/pricing/batch} → فقط ADMIN و PRICER (نوشتن)</li>
 *   <li>{@code PUT /api/v1/pricing/reorder} → فقط ADMIN و PRICER (چیدمانِ میز)</li>
 *   <li>{@code GET /api/v1/pricing/logs} → فقط ADMIN</li>
 * </ul>
 * محافظت در سطحِ مسیر انجام می‌شود، نه فقط پنهان‌کردنِ دکمه در UI —
 * تلاشِ مستقیمِ کارشناسِ فروش رویِ API باید ۴۰۳ بگیرد.
 */
@RestController
@RequestMapping("/api/v1/pricing")
public class PricingWorkspaceController {

    private final PricingWorkspaceService service;
    private final ActivityLogService activityLog;
    private final org.example.shop1.model.service.marketplace.FloorPriceService floorPriceService;

    public PricingWorkspaceController(PricingWorkspaceService service, ActivityLogService activityLog,
                                      org.example.shop1.model.service.marketplace.FloorPriceService floorPriceService) {
        this.service = service;
        this.activityLog = activityLog;
        this.floorPriceService = floorPriceService;
    }

    // ==========================================================
    // کفِ قیمتِ رقبا (بخشِ ۲)
    // همه زیرِ /api/v1/pricing/** هستند، پس فقط ADMIN/PRICER/SALES می‌بینند؛
    // نوشتن‌ها (POST) طبقِ قاعده‌ی SecurityConfig فقط ADMIN/PRICER.
    // ==========================================================

    /** آدرسِ جست‌وجویِ آماده — برایِ دکمهٔ «بازکردن در ترب» (نیمه‌خودکار). */
    @GetMapping("/marketplace/search-url")
    public ResponseEntity<Map<String, String>> searchUrl(@RequestParam String market,
                                                        @RequestParam String query) {
        return ResponseEntity.ok(Map.of("url", floorPriceService.searchPageUrl(market, query)));
    }

    /** مرحلهٔ A — نامزدها برایِ تأییدِ انسان. هرگز خودکار پذیرفته نمی‌شوند. */
    @PostMapping("/marketplace/candidates")
    public ResponseEntity<Map<String, Object>> candidates(@RequestBody Map<String, Object> body) {
        String market = String.valueOf(body.getOrDefault("market", "digikala"));
        String query = String.valueOf(body.getOrDefault("query", "")).trim();
        return ResponseEntity.ok(Map.of(
                "candidates", floorPriceService.searchCandidates(market, query, 5)));
    }

    /** ذخیرهٔ هویتِ تأییدشده (DKP). */
    @PostMapping("/marketplace/link")
    public ResponseEntity<Map<String, Object>> link(@RequestBody Map<String, String> body) {
        floorPriceService.linkProduct(
                body.get("productId"), body.getOrDefault("market", "digikala"),
                body.get("externalId"), body.get("url"));
        return ResponseEntity.ok(Map.of("status", "linked"));
    }

    /** مرحلهٔ B — به‌روزرسانیِ یک محصول. */
    @PostMapping("/marketplace/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(floorPriceService.refresh(
                String.valueOf(body.get("productId")),
                String.valueOf(body.getOrDefault("market", "digikala")),
                Boolean.TRUE.equals(body.get("force"))));
    }

    /** به‌روزرسانیِ همه — فقط محصولاتی که هویتشان تأیید شده. */
    @PostMapping("/marketplace/refresh-all")
    public ResponseEntity<Map<String, Object>> refreshAll(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body == null ? Map.of() : body;
        return ResponseEntity.ok(floorPriceService.refreshAllLinked(
                String.valueOf(b.getOrDefault("market", "digikala")),
                Boolean.TRUE.equals(b.get("force"))));
    }

    @GetMapping("/rows")
    public ResponseEntity<List<PricingRowDto>> rows(
            @RequestParam(name = "q", required = false) String q) {
        return ResponseEntity.ok(service.rows(q));
    }

    /**
     * توانایی‌هایِ نقشِ کاربرِ فعلی — تا UI بداند کدام ستون را قفل کند.
     * مرزِ واقعی همچنان سمتِ سرور است؛ این فقط برایِ نمایش است.
     */
    @GetMapping("/capabilities")
    public ResponseEntity<Map<String, Object>> capabilities() {
        return ResponseEntity.ok(Map.of(
                "canEditBulkPrice", service.canEditPricerOnlyFields()));
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> batch(@RequestBody List<Map<String, Object>> changes) {
        return ResponseEntity.ok(service.applyBatch(changes));
    }

    /**
     * ترتیبِ دستیِ ردیف‌ها (درگ‌دراپ). کلِ ترتیب می‌آید، نه فقط ردیفِ جابه‌جاشده.
     * <p>
     * ⚠️ در SecurityConfig صریحاً به ADMIN/PRICER محدود شده. بدونِ آن قاعده،
     * قاعدهٔ عمومیِ {@code /api/v1/pricing/**} به کارشناسِ فروش هم اجازهٔ
     * نوشتن می‌داد — در حالی که چیدمانِ میز مشترک است و برایِ همه عوض می‌شود.
     */
    @PutMapping("/reorder")
    public ResponseEntity<Map<String, Object>> reorder(
            @RequestBody List<org.example.shop1.model.dto.WorkspaceOrderDto> items) {
        return ResponseEntity.ok(Map.of("updated", service.reorder(items)));
    }

    @GetMapping("/logs")
    public ResponseEntity<Page<ActivityLog>> logs(
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "entityType", required = false) String entityType,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {

        Instant f = parseInstant(from);
        Instant t = parseInstant(to);
        return ResponseEntity.ok(activityLog.search(username, entityType, f, t, page, size));
    }

    /**
     * لاگِ فقط-محصولات — برایِ پنلِ فروشِ حضوری (ADMIN/PRICER/SALES).
     * <p>
     * {@code entityType} سمتِ <b>سرور</b> به PRODUCT قفل می‌شود، نه فیلترِ UI:
     * فروشنده باید بفهمد چرا قیمتِ کالایی عوض شده، ولی رویدادهایِ مدیریتی
     * (تغییرِ ضریب، ساخت/حذفِ کاربر) نباید در آن پنل دیده شوند.
     */
    @GetMapping("/logs/products")
    public ResponseEntity<Page<ActivityLog>> productLogs(
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {

        return ResponseEntity.ok(activityLog.search(
                username, ActivityLogService.ENTITY_PRODUCT,
                parseInstant(from), parseInstant(to), page, size));
    }

    /** تاریخِ ورودی می‌تواند ISO کامل یا فقط yyyy-MM-dd باشد. */
    private Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim();
        try {
            return Instant.parse(s);
        } catch (Exception ignored) {
            try {
                return java.time.LocalDate.parse(s).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
            } catch (Exception e) {
                return null;
            }
        }
    }
}
