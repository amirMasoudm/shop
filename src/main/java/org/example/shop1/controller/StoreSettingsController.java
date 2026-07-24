package org.example.shop1.controller;

import org.example.shop1.model.entity.StoreSettings;
import org.example.shop1.model.service.StoreSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings")
@CrossOrigin
public class StoreSettingsController {

    private final StoreSettingsService settingsService;

    public StoreSettingsController(StoreSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/store-location")
    public ResponseEntity<StoreSettings> getStoreLocation() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PostMapping("/store-location")
    public ResponseEntity<StoreSettings> saveStoreLocation(@RequestBody StoreSettings settings) {
        return ResponseEntity.ok(settingsService.updateSettings(settings));
    }

    // آستانه‌ی RFQ برای فروشگاه (عمومی) — فرانت با آن دکمه‌ی «درخواست پیش‌فاکتور» را شرطی می‌کند
    @GetMapping("/rfq-threshold")
    public ResponseEntity<Map<String, BigDecimal>> getRfqThreshold() {
        BigDecimal t = settingsService.getSettings().getRfqThreshold();
        return ResponseEntity.ok(Collections.singletonMap("threshold", t != null ? t : BigDecimal.ZERO));
    }

    // تنظیم آستانه — فقط ادمین (مسیر admin در SecurityConfig محافظت شده)
    @PostMapping("/admin/rfq-threshold")
    public ResponseEntity<StoreSettings> setRfqThreshold(@RequestBody Map<String, BigDecimal> body) {
        BigDecimal threshold = body.get("threshold");
        return ResponseEntity.ok(settingsService.updateRfqThreshold(threshold));
    }
}