package org.example.shop1.config;

import org.example.shop1.model.service.analytics.PageViewInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

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

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // ایجاد مسیر استاندارد برای فایل‌های هارد دیسک
        String uploadPath = Paths.get(uploadDir).toAbsolutePath().toUri().toString();

        System.out.println("Serving files from: " + uploadPath);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }
}