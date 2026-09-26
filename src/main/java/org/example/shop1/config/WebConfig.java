package org.example.shop1.config;

import org.example.shop1.model.service.analytics.PageViewInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;
import java.time.Duration;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // خواندن آدرس از فایل properties
    @Value("${app.upload.dir}")
    private String uploadDir;

    private final PageViewInterceptor pageViewInterceptor;

    public WebConfig(PageViewInterceptor pageViewInterceptor) {
        this.pageViewInterceptor = pageViewInterceptor;
    }

    /**
     * ⚠️ اینجا فقط اینترسپتورِ <b>ثبت</b> وصل می‌شود. برقراریِ هویت کارِ
     * {@code VisitorIdentityFilter} است که فیلتر است و روی همه‌چیز اجرا می‌شود —
     * این تفکیک عمدی است، نگاه کن به توضیحِ همان کلاس.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(pageViewInterceptor);
    }

    /**
     * کشِ فایل‌های استاتیک — گزینهٔ «الف»: کوتاه و امن، <b>بدونِ</b> نسخه‌گذاری.
     * <p>
     * 🔴 <b>چرا لازم شد:</b> اسپرینگ سکیوریتی روی <i>همهٔ</i> پاسخ‌ها
     * {@code no-cache, no-store} می‌گذارد، پس مرورگر هیچ جاوااسکریپت و سی‌اس‌اس و
     * عکسی را نگه نمی‌داشت و هر بازدید همه را دوباره دانلود می‌کرد. آن هدر فقط وقتی
     * نوشته می‌شود که پاسخ خودش {@code Cache-Control} نداشته باشد — پس کافی است
     * همین هندلرها سیاستِ خودشان را بگذارند.
     * <p>
     * ⚠️ <b>چرا یک ساعت و نه یک ماه:</b> نامِ فایل‌ها نسخه ندارد
     * ({@code pricing-workspace.js}، نه {@code pricing-workspace.<hash>.js}). کشِ
     * طولانی یعنی کارشناس بعد از دیپلوی تا یک ماه نسخهٔ کهنه را می‌دید و کسی
     * نمی‌فهمید چرا. سقفِ کهنگی اینجا یک ساعت است، و بعدش مرورگر با
     * {@code If-Modified-Since} می‌پرسد و اگر فایل عوض نشده بود فقط ۳۰۴ می‌گیرد.
     * کشِ طولانی + {@code immutable} فقط همراهِ نسخه‌گذاری معنی دارد (گزینهٔ «ب»).
     * <p>
     * ⚠️ صفحه‌های پنل ({@code Admin.html}، {@code SalesPanel.html}،
     * {@code AdminLogin.html}، {@code AnbarMali.html}، {@code Financial.html}) و
     * {@code /fragments/} عمداً اینجا نیستند: از هندلرِ پیش‌فرضِ بوت سرو می‌شوند و
     * همان {@code no-store}ِ سکیوریتی را می‌گیرند — همین چند روز چند بار دیپلوی
     * شده‌اند و نسخهٔ کهنه‌شان یعنی پنلِ خراب.
     */
    private static final CacheControl STATIC_ASSETS =
            CacheControl.maxAge(Duration.ofHours(1)).mustRevalidate().cachePublic();

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/js/**").addResourceLocations("classpath:/static/js/")
                .setCacheControl(STATIC_ASSETS);
        // فونت‌ها امروز زیرِ /css/fonts/ هستند و همین قاعده پوششان می‌دهد
        registry.addResourceHandler("/css/**").addResourceLocations("classpath:/static/css/")
                .setCacheControl(STATIC_ASSETS);
        registry.addResourceHandler("/img/**").addResourceLocations("classpath:/static/img/")
                .setCacheControl(STATIC_ASSETS);
        // پوشهٔ /fonts/ فعلاً وجود ندارد؛ برایِ روزی که بیاید
        registry.addResourceHandler("/fonts/**").addResourceLocations("classpath:/static/fonts/")
                .setCacheControl(STATIC_ASSETS);
        registry.addResourceHandler("/favicon.ico").addResourceLocations("classpath:/static/")
                .setCacheControl(STATIC_ASSETS);

        // ایجاد مسیر استاندارد برای فایل‌های هارد دیسک
        String uploadPath = Paths.get(uploadDir).toAbsolutePath().toUri().toString();

        System.out.println("Serving files from: " + uploadPath);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }
}