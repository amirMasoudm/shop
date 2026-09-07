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
        // همین محافظت برای فاصله‌ی چرخشِ بنر
        if (settings.getBannerRotationSeconds() == null) {
            settings.setBannerRotationSeconds(current.getBannerRotationSeconds());
        }
        // و برای ساعتِ کاریِ چت — ذخیره‌ی موقعیتِ فروشگاه نباید ساعتِ کاری را پاک کند
        if (settings.getChatWorkingDays() == null) {
            settings.setChatWorkingDays(current.getChatWorkingDays());
        }
        if (settings.getChatStartTime() == null) {
            settings.setChatStartTime(current.getChatStartTime());
        }
        if (settings.getChatEndTime() == null) {
            settings.setChatEndTime(current.getChatEndTime());
        }
        if (settings.getChatTimeZone() == null) {
            settings.setChatTimeZone(current.getChatTimeZone());
        }
        return settingsRepo.save(settings);
    }

    /**
     * ساعتِ کاریِ چت — فقط ADMIN (مسیرِ {@code /settings/admin/**}).
     * <p>
     * فهرستِ خالیِ روزها یعنی «همیشه باز»؛ عمداً مجاز است تا فروشگاهی که ساعتِ کاری
     * ندارد مجبور نباشد مقدارِ ساختگی بگذارد.
     */
    public StoreSettings updateChatHours(java.util.List<Integer> days, String start, String end, String timeZone) {
        if (days != null) {
            for (Integer d : days) {
                if (d == null || d < 1 || d > 7) {
                    throw new org.example.shop1.exeption.ApiException(
                            org.springframework.http.HttpStatus.BAD_REQUEST,
                            "شمارهٔ روز باید بین ۱ (دوشنبه) تا ۷ (یک‌شنبه) باشد");
                }
            }
        }
        requireHhMm(start, "ساعتِ شروع");
        requireHhMm(end, "ساعتِ پایان");
        if (timeZone != null && !timeZone.isBlank()) {
            try {
                java.time.ZoneId.of(timeZone.trim());
            } catch (Exception e) {
                throw new org.example.shop1.exeption.ApiException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "منطقهٔ زمانی نامعتبر است");
            }
        }

        StoreSettings current = getSettings();
        current.setId(SETTINGS_ID);
        current.setChatWorkingDays(days);
        current.setChatStartTime(blankToNull(start));
        current.setChatEndTime(blankToNull(end));
        current.setChatTimeZone(blankToNull(timeZone));
        return settingsRepo.save(current);
    }

    private void requireHhMm(String value, String label) {
        if (value == null || value.isBlank()) return;
        try {
            java.time.LocalTime.parse(value.trim());
        } catch (Exception e) {
            throw new org.example.shop1.exeption.ApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, label + " باید به شکلِ HH:mm باشد");
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
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

    /** فاصله‌ی مؤثرِ چرخشِ بنر (با پیش‌فرض). */
    public int getBannerRotationSeconds() {
        return getSettings().effectiveBannerRotationSeconds();
    }

    // تنظیم فقط فاصله‌ی چرخشِ بنر (بدون دست‌زدن به بقیه‌ی فیلدها)
    public StoreSettings updateBannerRotationSeconds(Integer seconds) {
        if (seconds == null || seconds <= 0) {
            throw new org.example.shop1.exeption.ApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "فاصله‌ی چرخش باید عددی بزرگ‌تر از صفر باشد");
        }
        StoreSettings current = getSettings();
        current.setId(SETTINGS_ID);
        current.setBannerRotationSeconds(seconds);
        return settingsRepo.save(current);
    }
}
