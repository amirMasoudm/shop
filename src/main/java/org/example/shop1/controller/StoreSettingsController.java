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
    private final org.example.shop1.model.service.ActivityLogService activityLog;

    public StoreSettingsController(StoreSettingsService settingsService,
                                   org.example.shop1.model.service.ActivityLogService activityLog) {
        this.settingsService = settingsService;
        this.activityLog = activityLog;
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

    /**
     * ضریبِ قیمتِ سایت — خواندنی برای میزِ کار (تا مقدارِ پیشنهادی را نشان دهد).
     * زیرِ /admin/ نیست چون PRICER هم باید بتواند ببیند، فقط نتواند تغییر دهد.
     */
    @GetMapping("/site-price-factor")
    public ResponseEntity<Map<String, BigDecimal>> getSitePriceFactor() {
        return ResponseEntity.ok(Collections.singletonMap("factor", settingsService.getSitePriceFactor()));
    }

    /**
     * تغییرِ ضریب — فقط ADMIN (مسیرِ /settings/admin/** در SecurityConfig محافظت شده؛
     * PRICER از API هم ۴۰۳ می‌گیرد، نه فقط پنهان‌شدنِ فیلد در UI).
     * یک قراردادِ تجاری است، پس تغییرش لاگ می‌شود.
     */
    @PostMapping("/admin/site-price-factor")
    public ResponseEntity<StoreSettings> setSitePriceFactor(@RequestBody Map<String, BigDecimal> body) {
        BigDecimal oldFactor = settingsService.getSitePriceFactor();
        BigDecimal newFactor = body.get("factor");
        StoreSettings saved = settingsService.updateSitePriceFactor(newFactor);

        activityLog.record(
                org.example.shop1.model.entity.ActivityLog.Action.PRICE_CHANGE,
                org.example.shop1.model.entity.ActivityLog.Source.MANUAL,
                "SETTINGS", "store_settings", "تنظیماتِ فروشگاه",
                "sitePriceFactor", oldFactor, saved.getSitePriceFactor());

        return ResponseEntity.ok(saved);
    }
}