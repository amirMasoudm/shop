package org.example.shop1.model.service.analytics;

import org.example.shop1.config.AnalyticsProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * تشخیصِ خزنده از روی User-Agent — تنها جایی که این قضاوت انجام می‌شود.
 * <p>
 * 🔴 <b>چرا لازم شد:</b> {@link VisitorIdentityFilter} برایِ هر درخواستِ بی‌کوکی یک
 * شناسهٔ تازه می‌سازد. خزنده کوکی را برنمی‌گرداند، پس هر درخواستِ گوگل‌بات یک
 * «بازدیدکنندهٔ یکتای تازه» با سشنِ تک‌صفحه‌ای و پرشِ صددرصد می‌ساخت. تا وقتی سرور
 * فقط با آی‌پی در دسترس بود این بی‌اثر بود؛ از سوییچِ دامنه، یک خزشِ کاملِ نقشهٔ سایت
 * یعنی صدها بازدیدکنندهٔ جعلی در یک نشست.
 * <p>
 * ⚠️ <b>User-Agentِ خالی ربات نیست.</b> بعضی پروکسی‌ها هدر را حذف می‌کنند و اگر خالی
 * را ربات بگیریم مشتریِ واقعی از آمار حذف می‌شود — خطایی که جهتش بدتر از آلودگیِ
 * رباتی است، چون آلودگی قابلِ پانویس‌کردن است ولی مشتریِ حذف‌شده برنمی‌گردد. خالی =
 * انسانِ ناشناس.
 * <p>
 * تشخیص عمداً مطابقتِ زیررشته‌ایِ ساده است، نه رجکس و نه کتابخانه: فهرست از
 * {@code app.analytics.crawler-tokens} می‌آید، پس افزودنِ خزندهٔ تازه فقط تغییرِ
 * پراپرتی است و استقرارِ دوباره نمی‌خواهد.
 */
@Component
public class CrawlerDetector {

    /** نشانه‌ها یک‌بار کوچک و تمیز می‌شوند؛ مسیرِ درخواست فقط مقایسه می‌کند. */
    private final List<String> tokens;

    public CrawlerDetector(AnalyticsProperties props) {
        this.tokens = props.getCrawlerTokens().stream()
                .filter(t -> t != null && !t.isBlank())
                .map(t -> t.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    /**
     * @param userAgent هدرِ خام؛ {@code null} یا خالی یعنی انسانِ ناشناس، نه ربات.
     */
    public boolean isCrawler(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return false;
        String ua = userAgent.toLowerCase(Locale.ROOT);
        for (String token : tokens) {
            if (ua.contains(token)) return true;
        }
        return false;
    }

    /** فقط برایِ لاگ و تشخیصِ خرابی — تعدادِ نشانه‌هایی که واقعاً بار شده‌اند. */
    public int tokenCount() {
        return tokens.size();
    }
}
