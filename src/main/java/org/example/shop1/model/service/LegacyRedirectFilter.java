package org.example.shop1.model.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.shop1.model.entity.LegacyRedirect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * ۳۰۱ برایِ آدرس‌های قدیمیِ ایندکس‌شده.
 *
 * <h3>چرا فیلتر و نه یک هندلرِ ۴۰۴</h3>
 * اولین پیاده‌سازی یک {@code @ControllerAdvice} روی استثنایِ «مسیر پیدا نشد» بود —
 * تمیزتر به‌نظر می‌رسید چون ذاتاً نمی‌توانست جلوی مسیرِ واقعی را بگیرد. ولی در تستِ
 * واقعی هر آدرسِ قدیمی <b>۴۰۱</b> گرفت، نه ۳۰۱: قاعدهٔ
 * {@code anyRequest().authenticated()} پیش از مسیریابی جلوی درخواست را می‌گیرد، پس
 * استثنایی که منتظرش بودیم اصلاً پرتاب نمی‌شد. گوگل هم ۴۰۱ می‌دید، نه ۳۰۱.
 * <p>
 * پس تشخیص باید <b>پیش از</b> زنجیرهٔ امنیت انجام شود. بهایش این است که دیگر
 * نمی‌شود به «اول بگذار اپ تلاش کند» تکیه کرد — و همین است که
 * {@link LegacyRedirectService#isReservedPath} را ضروری می‌کند: هیچ مسیری که به اپ
 * تعلق دارد اجازهٔ ثبت‌شدن به‌عنوان مبدأ را ندارد.
 *
 * <h3>هزینه</h3>
 * روی هر درخواست فقط یک نرمال‌سازی و یک {@code contains} روی مجموعهٔ درون‌حافظه‌ای
 * انجام می‌شود. دیتابیس تنها وقتی خوانده می‌شود که کلید واقعاً وجود داشته باشد.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class LegacyRedirectFilter extends OncePerRequestFilter {

    private final LegacyRedirectService service;

    public LegacyRedirectFilter(LegacyRedirectService service) {
        this.service = service;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (shouldTry(request)) {
            try {
                String key = LegacyRedirectService.normalize(request.getRequestURI());
                // 🔴 مسیرهای دیرحل عمداً اینجا رد می‌شوند. این فیلتر پیش از مسیریابیِ
                // اسپرینگ اجرا می‌شود و نمی‌داند مقالهٔ زنده‌ای با این اسلاگ هست یا نه؛
                // اگر اینجا ۳۰۱ می‌داد، یک ریدایرکتِ کهنه می‌توانست مقالهٔ سالم را بدزدد.
                // کنترلرِ مقاله همین رکورد را بعد از شکستِ جست‌وجو اعمال می‌کند.
                if (service.mightRedirect(key) && !LegacyRedirectService.isLateResolved(key)) {
                    Optional<LegacyRedirect> hit = service.resolve(key);
                    if (hit.isPresent()) {
                        send301(response, hit.get());
                        service.countHit(hit.get().getFromPath());
                        return;
                    }
                }
            } catch (Exception e) {
                // ریدایرکتِ قدیمی هرگز نباید جلوی خودِ سایت را بگیرد
                logger.debug("بررسیِ ریدایرکتِ قدیمی ناموفق بود: " + e);
            }
        }
        chain.doFilter(request, response);
    }

    /** فقط خواندن؛ یک POSTِ گم‌شده نباید به ۳۰۱ تبدیل شود. */
    private boolean shouldTry(HttpServletRequest request) {
        String m = request.getMethod();
        return "GET".equalsIgnoreCase(m) || "HEAD".equalsIgnoreCase(m);
    }

    /**
     * 🔴 <b>۳۰۱ و نه ۳۰۲.</b> انتقالِ دائمی است و گوگل باید همین را بفهمد تا اعتبارِ
     * صفحهٔ قدیمی به تازه منتقل شود؛ ۳۰۲ یعنی «آدرسِ قدیمی را نگه دار».
     */
    private void send301(HttpServletResponse response, LegacyRedirect r) {
        response.setStatus(HttpStatus.MOVED_PERMANENTLY.value());
        response.setHeader(HttpHeaders.LOCATION, LegacyRedirectService.toLocationHeader(r.getToPath()));
        // ۳۰۱ را مرورگرها تهاجمی و گاهی دائمی کش می‌کنند. تا وقتی نگاشت‌ها در حالِ
        // تنظیم‌اند، کشِ کوتاه اجازه می‌دهد اشتباه قابلِ اصلاح بماند.
        response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=3600");
    }
}
