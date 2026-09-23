package org.example.shop1.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;

/**
 * شکلِ پاسخِ خطا به مسیر بسته شود، نه به هدرِ {@code Accept}.
 * <p>
 * 🔴 <b>چرا لازم است:</b> {@code BasicErrorController} دو متد دارد — یکی
 * {@code produces=text/html} و یکی JSON — و انتخاب بینشان به {@code Accept}ِ کلاینت
 * واگذار می‌شود. نتیجه‌اش این بود که همان مسیرِ مرده به مرورگر HTML می‌داد و به
 * {@code curl} (که پیش‌فرض «هرچیزی» می‌فرستد) JSONِ خام. یعنی <b>تستِ خودکار
 * هرگز صفحه‌ای را که ساخته‌ایم نمی‌دید</b> و رفتارِ سایت به سلیقهٔ کلاینت وابسته بود.
 * <p>
 * قاعده: مسیرهای {@code /api/**} همیشه JSON، بقیه همیشه HTML.
 * <p>
 * ⚠️ این فیلتر عمداً <b>فقط روی ERROR dispatch</b> و فقط روی خودِ {@code /error}
 * ثبت می‌شود. روی درخواستِ عادی اجرا نمی‌شود، پس هیچ مسیرِ سالمی را لمس نمی‌کند.
 * هیچ منطقی از فالبکِ ۴۰۴ اینجا تکرار نشده — فقط یک هدر بازنویسی می‌شود.
 */
@Configuration
public class ErrorResponseTypeConfig {

    @Bean
    public FilterRegistrationBean<ErrorAcceptFilter> errorAcceptFilter() {
        FilterRegistrationBean<ErrorAcceptFilter> reg = new FilterRegistrationBean<>(new ErrorAcceptFilter());
        reg.setDispatcherTypes(DispatcherType.ERROR);
        reg.addUrlPatterns("/error");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        reg.setName("errorAcceptFilter");
        return reg;
    }

    public static class ErrorAcceptFilter extends OncePerRequestFilter {

        /** بدونِ این، OncePerRequestFilter اصلاً روی ERROR dispatch اجرا نمی‌شود. */
        @Override
        protected boolean shouldNotFilterErrorDispatch() {
            return false;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain chain) throws ServletException, IOException {
            String original = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
            String wanted = isApiPath(original) ? MediaType.APPLICATION_JSON_VALUE : MediaType.TEXT_HTML_VALUE;
            chain.doFilter(new FixedAcceptRequest(request, wanted), response);
        }

        private boolean isApiPath(String uri) {
            if (uri == null) return false;
            String p = uri.toLowerCase(Locale.ROOT);
            return p.equals("/api") || p.startsWith("/api/");
        }
    }

    /** فقط هدرِ {@code Accept} را جایگزین می‌کند؛ بقیهٔ درخواست دست‌نخورده است. */
    private static class FixedAcceptRequest extends HttpServletRequestWrapper {

        private final String accept;

        FixedAcceptRequest(HttpServletRequest request, String accept) {
            super(request);
            this.accept = accept;
        }

        @Override
        public String getHeader(String name) {
            return HttpHeaders.ACCEPT.equalsIgnoreCase(name) ? accept : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return HttpHeaders.ACCEPT.equalsIgnoreCase(name)
                    ? Collections.enumeration(Collections.singletonList(accept))
                    : super.getHeaders(name);
        }
    }
}
