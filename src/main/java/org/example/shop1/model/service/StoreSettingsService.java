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
        return settingsRepo.save(settings);
    }
}
