package org.example.shop1.model.service;

import org.example.shop1.model.dto.ShippingOption;
import org.example.shop1.model.entity.Address;
import org.example.shop1.model.entity.StoreSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ShippingService {

    private static final Logger log = LoggerFactory.getLogger(ShippingService.class);
    private final StoreSettingsService settingsService;

    // =========================================================================
    //   بخش تنظیم تعرفه‌های حمل و نقل (تمامی مبالغ به تومان است)
    // =========================================================================

    // ۱. تعرفه‌های پایه پستی (برای مرسوله‌های تا ۵۰۰ گرم)
    private static final long PRICE_POST_INTRA_CITY = 38000L;     // درون‌شهری (مثلاً از اصفهان به اصفهان)
    private static final long PRICE_POST_INTRA_PROVINCE = 48000L; // درون‌استانی (بین‌شهری هم‌استان، مثلاً اصفهان به نجف‌آباد)
    private static final long PRICE_POST_INTER_PROVINCE = 59000L; // برون‌استانی (استان متفاوت، مثلاً اصفهان به تهران)

    // ۲. هزینه‌های اضافه وزن (به ازای هر ۵۰۰ گرم وزن مازاد بر ۵۰۰ گرم پایه)
    private static final double BASE_WEIGHT_LIMIT = 500.0;         // وزن پایه به گرم
    private static final long EXTRA_WEIGHT_FEE_INTRA = 5000L;      // مازاد درون‌شهری و درون‌استانی (به ازای هر نیم کیلو)
    private static final long EXTRA_WEIGHT_FEE_INTER = 8000L;      // مازاد برون‌استانی (به ازای هر نیم کیلو)

    // ۳. تعرفه‌های پیک موتوری (فقط مخصوص همشهری‌ها)
    private static final long PRICE_PEIK_BASE = 55000L;           // هزینه پایه پیک موتوری
    private static final long PRICE_PEIK_PER_KG = 4000L;          // هزینه اضافه وزن پیک به ازای هر کیلوگرم

    // ۴. تعرفه‌های تیپاکس (ارسال سریع آفلاین)
    private static final long PRICE_TIPAX_BASE = 75000L;          // هزینه پایه تیپاکس
    private static final long PRICE_TIPAX_PER_KG = 6000L;         // هزینه اضافه وزن تیپاکس به ازای هر کیلوگرم

    // =========================================================================

    public ShippingService(StoreSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public List<ShippingOption> calculateOptions(Address destination, Double totalWeight) {
        List<ShippingOption> options = new ArrayList<>();
        StoreSettings origin = settingsService.getSettings();

        if (origin == null || destination == null) {
            log.warn("تنظیمات فروشگاه یا آدرس مقصد خالی است. تعرفه پیش‌فرض اعمال می‌شود.");
            options.add(new ShippingOption("POST_PISHTAZ", "پست پیشتاز (تعرفه ثابت)", PRICE_POST_INTER_PROVINCE, "۳ تا ۵ روز کاری"));
            return options;
        }

        double weight = (totalWeight != null && totalWeight > 0) ? totalWeight : 200.0; // وزن پیش‌فرض ۲۰۰ گرم

        // نرمال‌سازی اسامی برای مقایسه دقیق
        String originState = normalize(origin.getState());
        String destState = normalize(destination.getState());
        String originCity = normalize(origin.getCity());
        String destCity = normalize(destination.getCity());

        log.info("محاسبه لوکال کرایه از [{} - {}] به [{} - {}] برای وزن {} گرم",
                originState, originCity, destState, destCity, weight);

        // سناریو ۱: درون‌شهری (مبدا و مقصد کاملاً یکسان هستند)
        if (originCity.equals(destCity) && originState.equals(destState)) {
            // پیک موتوری فعال می‌شود
            long peikCost = calculatePeikCost(weight);
            options.add(new ShippingOption("SNAPP_BOX", "پیک موتوری (اسنپ / الوپیک)", peikCost, "ارسال فوری (امروز)"));

            // پست پیشتاز درون‌شهری
            long postCost = calculatePostCost(PRICE_POST_INTRA_CITY, EXTRA_WEIGHT_FEE_INTRA, weight);
            options.add(new ShippingOption("POST_PISHTAZ", "پست پیشتاز (درون‌شهری)", postCost, "۱ تا ۲ روز کاری"));
        }
        // سناریو ۲: درون‌استانی (استان‌ها یکی هستند ولی شهرها متفاوت - مانند اصفهان به نجف‌آباد)
        else if (originState.equals(destState)) {
            long postCost = calculatePostCost(PRICE_POST_INTRA_PROVINCE, EXTRA_WEIGHT_FEE_INTRA, weight);
            options.add(new ShippingOption("POST_PISHTAZ", "پست پیشتاز (درون‌استانی)", postCost, "۲ تا ۳ روز کاری"));

            long tipaxCost = calculateTipaxCost(weight);
            options.add(new ShippingOption("TIPAX", "تیپاکس (سریع بین‌شهری)", tipaxCost, "۱ تا ۲ روز کاری"));
        }
        // سناریو ۳: برون‌استانی (استان‌ها متفاوت هستند - مانند اصفهان به تهران)
        else {
            long postCost = calculatePostCost(PRICE_POST_INTER_PROVINCE, EXTRA_WEIGHT_FEE_INTER, weight);
            options.add(new ShippingOption("POST_PISHTAZ", "پست پیشتاز (برون‌استانی)", postCost, "۳ تا ۵ روز کاری"));

            long tipaxCost = calculateTipaxCost(weight);
            options.add(new ShippingOption("TIPAX", "تیپاکس (ارسال سریع برون‌استانی)", tipaxCost, "۲ تا ۳ روز کاری"));
        }

        return options;
    }

    // فرمول محاسبه هزینه پست پیشتاز بر اساس وزن و تعرفه‌های پایه
    private long calculatePostCost(long basePrice, long extraWeightFee, double weight) {
        if (weight <= BASE_WEIGHT_LIMIT) {
            return basePrice;
        }
        // محاسبه تعداد ۵۰۰ گرم‌های مازاد
        double excessWeight = weight - BASE_WEIGHT_LIMIT;
        long steps = (long) Math.ceil(excessWeight / 500.0);
        return basePrice + (steps * extraWeightFee);
    }

    // فرمول محاسبه پیک موتوری بر اساس وزن مرسوله
    private long calculatePeikCost(double weight) {
        double weightKg = weight / 1000.0;
        if (weightKg <= 1.0) {
            return PRICE_PEIK_BASE;
        }
        long extraKg = (long) Math.ceil(weightKg - 1.0);
        return PRICE_PEIK_BASE + (extraKg * PRICE_PEIK_PER_KG);
    }

    // فرمول محاسبه تیپاکس بر اساس وزن مرسوله
    private long calculateTipaxCost(double weight) {
        double weightKg = weight / 1000.0;
        if (weightKg <= 1.0) {
            return PRICE_TIPAX_BASE;
        }
        long extraKg = (long) Math.ceil(weightKg - 1.0);
        return PRICE_TIPAX_BASE + (extraKg * PRICE_TIPAX_PER_KG);
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