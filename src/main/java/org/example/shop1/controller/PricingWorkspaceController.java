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

    public PricingWorkspaceController(PricingWorkspaceService service, ActivityLogService activityLog) {
        this.service = service;
        this.activityLog = activityLog;
    }

    @GetMapping("/rows")
    public ResponseEntity<List<PricingRowDto>> rows(
            @RequestParam(name = "q", required = false) String q) {
        return ResponseEntity.ok(service.rows(q));
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> batch(@RequestBody List<Map<String, Object>> changes) {
        return ResponseEntity.ok(service.applyBatch(changes));
    }

    @GetMapping("/logs")
    public ResponseEntity<Page<ActivityLog>> logs(
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {

        Instant f = parseInstant(from);
        Instant t = parseInstant(to);
        return ResponseEntity.ok(activityLog.search(username, f, t, page, size));
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
