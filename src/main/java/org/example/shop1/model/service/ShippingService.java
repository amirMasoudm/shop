package org.example.shop1.model.service;

import org.example.shop1.model.dto.ShippingOption;
import org.example.shop1.model.entity.Address;
import org.example.shop1.model.entity.StoreSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * لیستِ روش‌هایِ ارسالِ قابلِ‌انتخاب — بدونِ محاسبهٔ قیمت.
 * <p>
 * هزینهٔ ارسال («پس‌کرایه») در لحظهٔ تحویل توسطِ شرکتِ حمل‌ونقل از گیرنده گرفته
 * می‌شود، نه توسطِ سایت؛ این سرویس دیگر تعرفه حساب نمی‌کند، فقط بر اساسِ مبدأ/مقصد
 * و وزنِ مرسوله گزینه‌هایِ مرتبط را فیلتر می‌کند.
 */
@Service
public class ShippingService {

    private static final Logger log = LoggerFactory.getLogger(ShippingService.class);
    private final StoreSettingsService settingsService;

    // بالاتر از این وزن، «باربری» هم به‌عنوانِ گزینه اضافه می‌شود (برایِ مرسولاتِ حجیم/سنگین)
    private static final double BARBARI_WEIGHT_THRESHOLD_GRAMS = 5000.0;

    public ShippingService(StoreSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public List<ShippingOption> calculateOptions(Address destination, Double totalWeight) {
        List<ShippingOption> options = new ArrayList<>();
        StoreSettings origin = settingsService.getSettings();

        if (origin == null || destination == null) {
            log.warn("تنظیمات فروشگاه یا آدرس مقصد خالی است. فقط پست پیشتاز پیشنهاد می‌شود.");
            options.add(new ShippingOption("POST_PISHTAZ", "پست پیشتاز (پس‌کرایه)", "۳ تا ۵ روز کاری"));
            return options;
        }

        double weight = (totalWeight != null && totalWeight > 0) ? totalWeight : 200.0; // وزن پیش‌فرض ۲۰۰ گرم

        // نرمال‌سازی اسامی برای مقایسه دقیق
        String originState = normalize(origin.getState());
        String destState = normalize(destination.getState());
        String originCity = normalize(origin.getCity());
        String destCity = normalize(destination.getCity());

        log.info("گزینه‌هایِ ارسال از [{} - {}] به [{} - {}] برای وزن {} گرم",
                originState, originCity, destState, destCity, weight);

        boolean sameCity = originCity.equals(destCity) && originState.equals(destState);

        // پیک موتوری فقط برایِ درون‌شهری معنا دارد
        if (sameCity) {
            options.add(new ShippingOption("SNAPP_BOX", "پیک موتوری (اسنپ / الوپیک) — پس‌کرایه", "ارسال فوری (امروز)"));
        }

        options.add(new ShippingOption("POST_PISHTAZ", "پست پیشتاز — پس‌کرایه",
                sameCity ? "۱ تا ۲ روز کاری" : "۲ تا ۵ روز کاری"));
        options.add(new ShippingOption("TIPAX", "تیپاکس (سراسری) — پس‌کرایه",
                sameCity ? "۱ تا ۲ روز کاری" : "۲ تا ۳ روز کاری"));

        // مرسولاتِ حجیم/سنگین: باربری هم اضافه شود
        if (weight > BARBARI_WEIGHT_THRESHOLD_GRAMS) {
            options.add(new ShippingOption("BARBARI", "باربری (برایِ حجم/وزنِ بیشتر) — پس‌کرایه", "۲ تا ۴ روز کاری"));
        }

        return options;
    }

    // یکسان‌سازی کاراکترها برای جلوگیری از خطای فاصله یا ی/ک عربی
    private String normalize(String s) {
        if (s == null) return "";
        return s.replace("استان", "")
                .replace("ي", "ی")
                .replace("ك", "ک")
                .replace("آ", "ا")
                .replace(" ", "")
                .replace("‌", "") // حذف نیم‌فاصله‌ها
                .trim()
                .toLowerCase();
    }
}
