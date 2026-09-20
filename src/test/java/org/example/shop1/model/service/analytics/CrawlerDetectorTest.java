package org.example.shop1.model.service.analytics;

import org.example.shop1.config.AnalyticsProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * نگهبانِ فیلترِ ربات.
 * <p>
 * دو چیز اینجا آزموده می‌شود که هیچ‌کدام در تستِ دستیِ curl دیده نمی‌شوند:
 * <ul>
 *   <li><b>پیش‌فرض بدونِ پراپرتی کار کند</b> — اگر {@code app.analytics.crawler-tokens}
 *       از {@code application.properties} بیفتد، فیلتر باید همچنان ربات بگیرد. خرابیِ
 *       این حالت خاموش است: هیچ خطایی نمی‌دهد، فقط آمار دوباره آلوده می‌شود.</li>
 *   <li><b>User-Agentِ خالی انسان است</b> — جهتِ این خطا مهم است. ربات‌شمردنِ خالی
 *       یعنی حذفِ مشتریِ واقعی از آمار، که برگشت‌ناپذیر است؛ آلودگیِ رباتی دستِ‌کم
 *       با پانویس قابلِ تحمل است.</li>
 * </ul>
 */
class CrawlerDetectorTest {

    /** پیش‌فرضِ جاوایی — همان چیزی که بدونِ هیچ پراپرتی‌ای بار می‌شود. */
    private CrawlerDetector withDefaults() {
        return new CrawlerDetector(new AnalyticsProperties());
    }

    @Test
    void پیش‌فرض_بدونِ_پراپرتی_ربات_را_می‌گیرد() {
        CrawlerDetector d = withDefaults();
        assertTrue(d.tokenCount() > 0, "فهرستِ پیش‌فرض نباید خالی باشد");
        assertTrue(d.isCrawler("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"));
        assertTrue(d.isCrawler("AhrefsBot/7.0"));
        assertTrue(d.isCrawler("Mozilla/5.0 (compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm)"));
        assertTrue(d.isCrawler("facebookexternalhit/1.1"));
        assertTrue(d.isCrawler("curl/8.4.0"));
    }

    @Test
    void مرورگرِ_واقعی_ربات_حساب_نمی‌شود() {
        CrawlerDetector d = withDefaults();
        assertFalse(d.isCrawler("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"));
        assertFalse(d.isCrawler("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 "
                + "(KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"));
    }

    @Test
    void خالی_و_نال_انسانِ_ناشناس_اند_نه_ربات() {
        CrawlerDetector d = withDefaults();
        assertFalse(d.isCrawler(null), "نبودِ هدر یعنی ناشناس، نه ربات");
        assertFalse(d.isCrawler(""));
        assertFalse(d.isCrawler("   "));
    }

    @Test
    void مطابقت_به_بزرگ_و_کوچک_حساس_نیست() {
        CrawlerDetector d = withDefaults();
        assertTrue(d.isCrawler("GOOGLEBOT/2.1"));
        assertTrue(d.isCrawler("SomeThing-CRAWLER-9"));
    }

    @Test
    void پراپرتی_فهرست_را_جایگزین_می‌کند() {
        AnalyticsProperties props = new AnalyticsProperties();
        props.setCrawlerTokens(List.of("zzz-only-this"));
        CrawlerDetector d = new CrawlerDetector(props);

        assertEquals(1, d.tokenCount());
        assertTrue(d.isCrawler("agent zzz-only-this v1"));
        // گوگل‌بات دیگر در فهرست نیست — یعنی پراپرتی واقعاً جایگزین شده، نه اضافه
        assertFalse(d.isCrawler("Mozilla/5.0 (compatible; Googlebot/2.1)"));
    }
}
