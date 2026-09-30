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
    private final org.example.shop1.model.service.BusinessHoursService businessHours;

    public StoreSettingsController(StoreSettingsService settingsService,
                                   org.example.shop1.model.service.ActivityLogService activityLog,
                                   org.example.shop1.model.service.BusinessHoursService businessHours) {
        this.settingsService = settingsService;
        this.activityLog = activityLog;
        this.businessHours = businessHours;
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

    // فاصله‌ی چرخشِ خودکارِ اسلایدرِ بنرِ خانه (ثانیه) — خواندنی و عمومی (CL.html این را می‌خواند)
    @GetMapping("/banner-rotation-seconds")
    public ResponseEntity<Map<String, Integer>> getBannerRotationSeconds() {
        return ResponseEntity.ok(Collections.singletonMap("seconds", settingsService.getBannerRotationSeconds()));
    }

    // تنظیمِ فاصله — فقط ادمین
    @PostMapping("/admin/banner-rotation-seconds")
    public ResponseEntity<StoreSettings> setBannerRotationSeconds(@RequestBody Map<String, Integer> body) {
        return ResponseEntity.ok(settingsService.updateBannerRotationSeconds(body.get("seconds")));
    }

    /**
     * ساعتِ کاریِ چتِ پشتیبانی — خواندنی و عمومی.
     * <p>
     * عمومی است چون حبابِ چت باید <b>پیش از ورودِ کاربر</b> هم بتواند بگوید چه ساعتی
     * فعال می‌شود؛ سکوتِ بدونِ زمان بدتر از نبودنِ چت است.
     */
    @GetMapping("/chat-hours")
    public ResponseEntity<Map<String, Object>> getChatHours() {
        var s = settingsService.getSettings();
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("workingDays", s.getChatWorkingDays());
        out.put("startTime", s.getChatStartTime());
        out.put("endTime", s.getChatEndTime());
        out.put("timeZone", s.getChatTimeZone());
        out.put("status", businessHours.status());
        return ResponseEntity.ok(out);
    }

    /** تنظیمِ ساعتِ کاریِ چت — فقط ADMIN. */
    @PostMapping("/admin/chat-hours")
    public ResponseEntity<StoreSettings> setChatHours(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        java.util.List<Integer> days = (java.util.List<Integer>) body.get("workingDays");
        return ResponseEntity.ok(settingsService.updateChatHours(
                days,
                (String) body.get("startTime"),
                (String) body.get("endTime"),
                (String) body.get("timeZone")));
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

    /** ضریبِ «همکار تک» — خواندنی برای میزِ کار، مثلِ ضریبِ سایت. */
    @GetMapping("/partner-unit-factor")
    public ResponseEntity<Map<String, BigDecimal>> getPartnerUnitFactor() {
        return ResponseEntity.ok(Collections.singletonMap("factor", settingsService.getPartnerUnitFactor()));
    }

    @PostMapping("/admin/partner-unit-factor")
    public ResponseEntity<StoreSettings> setPartnerUnitFactor(@RequestBody Map<String, BigDecimal> body) {
        BigDecimal oldFactor = settingsService.getPartnerUnitFactor();
        StoreSettings saved = settingsService.updatePartnerUnitFactor(body.get("factor"));

        activityLog.record(
                org.example.shop1.model.entity.ActivityLog.Action.PRICE_CHANGE,
                org.example.shop1.model.entity.ActivityLog.Source.MANUAL,
                "SETTINGS", "store_settings", "تنظیماتِ فروشگاه",
                "partnerUnitFactor", oldFactor, saved.getPartnerUnitFactor());

        return ResponseEntity.ok(saved);
    }
}