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
        // اگر آستانه‌ی RFQ در ورودی نیامده باشد، مقدار قبلی حفظ شود تا ذخیره‌ی موقعیت فروشگاه آن را پاک نکند
        if (settings.getRfqThreshold() == null) {
            settings.setRfqThreshold(getSettings().getRfqThreshold());
        }
        return settingsRepo.save(settings);
    }

    // تنظیم فقط آستانه‌ی RFQ (بدون دست‌زدن به بقیه‌ی فیلدها)
    public StoreSettings updateRfqThreshold(java.math.BigDecimal threshold) {
        StoreSettings current = getSettings();
        current.setId(SETTINGS_ID);
        current.setRfqThreshold(threshold);
        return settingsRepo.save(current);
    }
}
