package org.example.shop1.model.service.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * جابِ شبانه: اول جمع‌بندیِ دیروز، بعد چرخهٔ آرشیو-سپس-حذف.
 * <p>
 * ترتیب عمدی است — جمع‌بندی باید پیش از هر حذفی انجام شود، وگرنه روزی که داده‌اش
 * آرشیو شده ممکن است بدونِ جمع‌بندی بماند و در نمودارِ چندساله حفره بسازد.
 * <p>
 * ⚠️ تک‌نودی است. اگر روزی دو نمونه از بک‌اند بالا بیاید، هر دو این جاب را اجرا
 * می‌کنند؛ جمع‌بندی چون idempotent است ضرری نمی‌بیند، ولی آرشیو باید آن روز قفلِ
 * توزیع‌شده بگیرد.
 */
@Component
public class AnalyticsNightlyJob {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsNightlyJob.class);

    private final DailyStatsService dailyStats;
    private final AnalyticsRetentionService retention;

    public AnalyticsNightlyJob(DailyStatsService dailyStats, AnalyticsRetentionService retention) {
        this.dailyStats = dailyStats;
        this.retention = retention;
    }

    /** ۰۳:۱۵ به وقتِ همان منطقهٔ زمانی‌ای که مرزِ روز را تعیین می‌کند. */
    @Scheduled(cron = "0 15 3 * * *", zone = "${app.analytics.zone:}")
    public void run() {
        try {
            dailyStats.rollup(LocalDate.now(dailyStats.zone()).minusDays(1));
        } catch (Exception e) {
            // شکستِ جمع‌بندی نباید جلوی آرشیو را بگیرد؛ آن یکی دادهٔ مالک را نگه می‌دارد.
            log.error("جمع‌بندیِ شبانه شکست خورد.", e);
        }
        try {
            retention.runRetention();
        } catch (Exception e) {
            log.error("چرخهٔ نگه‌داری شکست خورد.", e);
        }
    }
}
