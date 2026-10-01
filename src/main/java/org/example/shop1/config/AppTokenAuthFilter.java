package org.example.shop1.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.shop1.model.entity.AppToken;
import org.example.shop1.model.service.AppAuthService;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * سرآیندِ {@code Authorization: Bearer} را برای {@code /api/app/v1/**} می‌خواند.
 * <p>
 * 🔴 <b>هیچ نقشی نمی‌دهد</b> — نه ADMIN، نه PRICER، نه حتی USER. فقط «این درخواست از
 * طرفِ کاربرِ فلان است». پس توکنِ دزدیده‌شده هرگز به پنل یا APIهای سایت راه ندارد؛
 * این فیلتر فقط در زنجیرهٔ {@code /api/app/v1/**} نشسته و آن زنجیره هم جز
 * کنترلرِ خودش مقصدی ندارد.
 * <p>
 * 🔴 توکن هرگز لاگ نمی‌شود. عمداً بینِ «سرآیند نبود» و «توکن نامعتبر بود» فرقی گذاشته
 * نمی‌شود: هر دو یعنی ناشناس، و کنترلر ۴۰۱ِ قرارداد را برمی‌گرداند.
 * <p>
 * عمداً {@code @Component} نیست: بوت هر فیلترِ bean را روی <b>همهٔ</b> درخواست‌ها
 * ثبت می‌کرد.
 */
public class AppTokenAuthFilter extends OncePerRequestFilter {

    private final AppAuthService appAuth;

    public AppTokenAuthFilter(AppAuthService appAuth) {
        this.appAuth = appAuth;
    }

    /** هویتِ کاربرِ اپ: شناسهٔ کاربر و رکوردِ توکن، بدونِ هیچ authority. */
    public static class AppTokenAuthentication extends AbstractAuthenticationToken {
        private final AppToken token;

        public AppTokenAuthentication(AppToken token) {
            super(List.of());
            this.token = token;
            setAuthenticated(true);
        }

        public AppToken getToken() { return token; }
        @Override public Object getCredentials() { return null; }
        @Override public Object getPrincipal() { return token.getUserId(); }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            appAuth.findActive(header.substring(7).trim()).ifPresent(t -> {
                SecurityContext ctx = SecurityContextHolder.createEmptyContext();
                ctx.setAuthentication(new AppTokenAuthentication(t));
                SecurityContextHolder.setContext(ctx);
            });
        }
        chain.doFilter(request, response);
    }
}
