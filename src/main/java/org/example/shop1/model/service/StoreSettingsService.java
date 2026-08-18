package org.example.shop1.model.service;

import org.example.shop1.model.entity.StoreSettings;

import org.example.shop1.model.reposritory.StoreSettingsRepository;
import org.springframework.stereotype.Service;

@Service
public class StoreSettingsService {

    private final StoreSettingsRepository settingsRepo;
    private static final String SETTINGS_ID = "origin_location"; // شناسه ثابت

    public StoreSettingsService(StoreSettingsRepository settingsRepo) {
        this.settingsRepo = settingsRepo;
    }

    public StoreSettings getSettings() {
        return settingsRepo.findById(SETTINGS_ID)
                .orElse(new StoreSettings()); // اگر نبود یک آبجکت خالی برگردان
    }

    public StoreSettings updateSettings(StoreSettings settings) {
        settings.setId(SETTINGS_ID); // مطمئن می‌شویم که همیشه روی همان رکورد قبلی ذخیره می‌شود
        StoreSettings current = getSettings();
        // اگر آستانه‌ی RFQ در ورودی نیامده باشد، مقدار قبلی حفظ شود تا ذخیره‌ی موقعیت فروشگاه آن را پاک نکند
        if (settings.getRfqThreshold() == null) {
            settings.setRfqThreshold(current.getRfqThreshold());
        }
        // همین محافظت برای ضریبِ قیمت: ذخیره‌ی موقعیتِ فروشگاه نباید ضریب را پاک کند
        if (settings.getSitePriceFactor() == null) {
            settings.setSitePriceFactor(current.getSitePriceFactor());
        }
        return settingsRepo.save(settings);
    }

    /** ضریبِ مؤثرِ قیمتِ سایت (با پیش‌فرض) — نقطه‌ی واحدِ حقیقت برای همه‌ی محاسبات. */
    public java.math.BigDecimal getSitePriceFactor() {
        return getSettings().effectiveSitePriceFactor();
    }

    /** تنظیمِ ضریب — فقط ADMIN (مسیرِ /settings/admin/** در SecurityConfig محافظت شده). */
    public StoreSettings updateSitePriceFactor(java.math.BigDecimal factor) {
        if (factor == null || factor.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new org.example.shop1.exeption.ApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "ضریب باید عددی بزرگ‌تر از صفر باشد");
        }
        StoreSettings current = getSettings();
        current.setId(SETTINGS_ID);
        current.setSitePriceFactor(factor);
        return settingsRepo.save(current);
    }

    // تنظیم فقط آستانه‌ی RFQ (بدون دست‌زدن به بقیه‌ی فیلدها)
    public StoreSettings updateRfqThreshold(java.math.BigDecimal threshold) {
        StoreSettings current = getSettings();
        current.setId(SETTINGS_ID);
        current.setRfqThreshold(threshold);
        return settingsRepo.save(current);
    }
}
