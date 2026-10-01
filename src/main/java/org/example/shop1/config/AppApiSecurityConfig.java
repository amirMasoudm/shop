package org.example.shop1.config;

import org.example.shop1.model.service.AppAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * زنجیرهٔ امنیتیِ جدا برای {@code /api/app/v1/**} — مسیرهای اپِ اندرویدی و ابزارِ سایت.
 * قرارداد: {@code docs/dadehlink-api-contract.md}، بخشِ «CORS و CSRF».
 * <p>
 * چرا زنجیرهٔ جدا و نه چند خط در {@link SecurityConfig}: CORS و CSRFِ این پیشوند با
 * بقیهٔ سایت فرق دارد (مبدأ اپ، بدونِ credentials، بدونِ CSRF چون احراز با سرآیندِ توکن
 * است). اگر در همان زنجیره بود، هر تغییرِ این‌جا ریسکِ شکستنِ کلِ سایت را داشت.
 * {@code @Order(1)} یعنی این زنجیره پیش از زنجیرهٔ عمومی (که روی همهٔ مسیرهاست) انتخاب
 * می‌شود؛ بقیهٔ سایت دقیقاً مثلِ قبل می‌ماند.
 * <p>
 * ⚠️ CSRF این‌جا خاموش است ولی دو مسیر با کوکی کار می‌کنند ({@code verify} با
 * {@code client=web} سشن می‌سازد، {@code session-token} سشن می‌خواند). محافظشان:
 * کنترلر فقط {@code application/json} می‌پذیرد، پس سایتِ بیگانه بدونِ پیش‌پرواز
 * (preflight) نمی‌تواند صدایشان بزند، و پیش‌پرواز برای مبدأِ ناشناس رد می‌شود. کوکیِ
 * سشن هم {@code SameSite=Lax} است.
 */
@Configuration
public class AppApiSecurityConfig {

    @Value("${app.app-api.allowed-origins}")
    private String allowedOrigins;

    @Bean
    @Order(1)
    public SecurityFilterChain appApiFilterChain(HttpSecurity http, AppAuthService appAuth) throws Exception {
        http
                .securityMatcher("/api/app/v1/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(appApiCors()))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .addFilterBefore(new AppTokenAuthFilter(appAuth), AnonymousAuthenticationFilter.class)
                // دسترسی در خودِ کنترلر سنجیده می‌شود تا بدنهٔ ۴۰۱ همان JSONِ قرارداد باشد.
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }

    private UrlBasedCorsConfigurationSource appApiCors() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).toList());
        c.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        c.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        c.setAllowCredentials(false);
        c.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/app/v1/**", c);
        return source;
    }
}
