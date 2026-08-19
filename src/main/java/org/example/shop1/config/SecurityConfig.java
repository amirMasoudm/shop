
package org.example.shop1.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
// ۱. ابتدا این ایمپورت‌ها را بالای فایل SecurityConfig اضافه کنید:
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> {
                    CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
                    tokenRepository.setCookiePath("/");

                    CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
                    requestHandler.setCsrfRequestAttributeName(null);
                    csrf.csrfTokenRepository(tokenRepository)
                            .csrfTokenRequestHandler(requestHandler)
                            //  مسیر سفارشات را از چک کردن CSRF معاف کن
//                            .ignoringRequestMatchers("/api/orders/**")

                            // Torob API از سرورِ ترب می‌آید، نه از مرورگرِ کاربر: کوکیِ سشن و
                            // توکنِ CSRF ندارد و اصلاً نمی‌تواند داشته باشد. احرازِ هویتش با
                            // امضایِ ed25519 است، پس CSRF اینجا موضوعیت ندارد. بدونِ این معافیت
                            // درخواستِ ترب قبل از رسیدن به کنترلر ۴۰۳ می‌گرفت.
                            .ignoringRequestMatchers("/torob_api/**")
                            ;

                })
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // در SecurityConfig.java بخش authorizeHttpRequests

                .authorizeHttpRequests(auth -> auth
                        // ۱. حتما صفحه خطا را کاملا باز بگذارید
                        .requestMatchers("/error", "/favicon.ico", "/robots.txt", "/sitemap.xml").permitAll()

                        .requestMatchers("/", "/product/**", "/category/**", "/blog/**", "/CL.html", "/AdminLogin.html", "/customerPanel.html").permitAll()
                        // پوشهٔ واقعیِ تصاویرِ ثابت `/img/` است؛ `/images/**` هرگز وجود نداشت و
                        // به همین دلیل لوگو در کلِ سایت ۴۰۱ می‌گرفت.
                        .requestMatchers("/css/**", "/js/**", "/fonts/**", "/img/**", "/images/**", "/uploads/**").permitAll()

                        // ⛔ مسیرهای ادمینِ API باید «قبل از» permitAll عمومی بیایند (اولین match برنده است)
                        .requestMatchers("/api/v1/articles/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/products/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/rfq/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/settings/admin/**").hasRole("ADMIN")

                        // آستانه‌ی RFQ خواندنی و عمومی است (فرانت دکمه را شرطی می‌کند)
                        .requestMatchers(HttpMethod.GET, "/api/v1/settings/rfq-threshold").permitAll()

                        // نوشتن محصول فقط ادمین؛ جستجو (POST) عمومی می‌ماند چون فرانت با آن فیلتر می‌کند
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/search").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/products").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("ADMIN")

                        // ۲. مسیرهای عمومی API را با دقت بیشتر باز کنید (حذف HttpMethod.GET برای تست اگر جواب نداد)
                        .requestMatchers(
                                "/api/v1/products/**",
                                "/api/categories/**",
                                "/api/v1/landing-sections/**", // اینجا در لاگ خطا میداد
                                "/api/comments/product/**",
                                "/api/v1/articles/**",
                                "/api/settings/**"
                        ).permitAll()

                        // میزِ کارِ قیمت‌گذاری — محافظت در سطحِ مسیر، نه فقط پنهان‌کردنِ دکمه در UI.
                        // ترتیب مهم است: قواعدِ خاص‌ترِ نوشتن/لاگ باید قبل از قاعده‌ی کلیِ خواندن بیایند.
                        .requestMatchers(HttpMethod.GET, "/api/v1/pricing/logs").hasRole("ADMIN")
                        // کارشناسِ فروش هم می‌نویسد؛ تنها استثنا فیلدِ «فروش تعدادی» است که
                        // در PricingWorkspaceService به‌صورتِ فیلدی رد می‌شود (نه مسیری).
                        .requestMatchers(HttpMethod.POST, "/api/v1/pricing/batch").hasAnyRole("ADMIN", "PRICER", "SALES")
                        .requestMatchers(HttpMethod.POST, "/api/v1/pricing/marketplace/**").hasAnyRole("ADMIN", "PRICER", "SALES")
                        .requestMatchers("/api/v1/pricing/**").hasAnyRole("ADMIN", "PRICER", "SALES")

                        // Torob Product API — ترب خودش با JWTِ ed25519 احراز می‌شود، نه سشنِ ما.
                        // بدونِ این خط، قاعده‌ی anyRequest().authenticated() پایین بلاکش می‌کند
                        // (همان کلاسِ باگی که قبلاً روی /img/** خوردیم).
                        .requestMatchers(HttpMethod.POST, "/torob_api/v3/products").permitAll()

                        // ۳. باقی مسیرها
                        .requestMatchers("/api/auth/**").permitAll()

                        // صفحه‌ی پنل برای کارشناسانِ قیمت‌گذاری/فروش هم باز است، چون میزِ کارِ
                        // قیمت‌گذاری داخلِ همین فایل است. خودِ تب‌ها و اندپوینت‌ها جداگانه
                        // بر اساسِ نقش محدود می‌شوند (/api/v1/pricing/** و /api/users/admin/**)،
                        // پس بازبودنِ صفحه دسترسیِ اضافه‌ای نمی‌دهد.
                        .requestMatchers("/Admin.html").hasAnyRole("ADMIN", "PRICER", "SALES")

                        // پنلِ فروشِ حضوری — مسیرِ صفحه از همان اول تعریف می‌شود.
                        // ⚠️ دوبار در همین پروژه API محافظت شد ولی مسیرِ صفحه جا ماند
                        // (/img/** و ورودِ نقش‌هایِ جدید به Admin.html). تکرارش نمی‌کنیم.
                        .requestMatchers("/SalesPanel.html").hasAnyRole("ADMIN", "PRICER", "SALES")

                        // ۴. مسیرهای ادمین
                        .requestMatchers(
                                "/api/users/admin/**",
                                "/api/orders/admin/**",
                                "/api/comments/admin/**"
                        ).hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                // ۴. این بخش را اضافه کن: اگر کاربر لاگین نبود و خواست وارد Admin.html شود، به AdminLogin.html هدایت شود
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            // صفحه‌هایِ پنل (نه APIها) باید به لاگین هدایت شوند، نه ۴۰۱ خام.
                            // قبلاً فقط Admin.html بود و SalesPanel.html جا مانده بود: کاربر
                            // بعد از خروج، به‌جایِ صفحه‌ی ورود یک ۴۰۱ خالی می‌دید.
                            if (isPanelPage(request.getRequestURI())) {
                                response.sendRedirect("/AdminLogin.html");
                            } else {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                            }
                        })
                        // وقتی شخص لاگین کرده ولی نقشش اجازه‌ی این صفحه را ندارد
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (isPanelPage(request.getRequestURI())) {
                                response.sendRedirect("/AdminLogin.html");
                            } else {
                                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
                            }
                        })
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(
                                SessionManagementConfigurer.SessionFixationConfigurer::migrateSession
                        )
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .toList()
        );

        configuration.setAllowedMethods(
                List.of("GET","POST","PUT","DELETE","OPTIONS")
        );

        configuration.setAllowedHeaders(List.of("*"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * صفحه‌هایِ پنلِ داخلی که کاربرِ بدونِ دسترسی باید به صفحه‌ی ورود هدایت شود،
     * نه اینکه ۴۰۱/۴۰۳ خام ببیند. APIها عمداً اینجا نیستند (کلاینتشان باید کدِ
     * وضعیت را بگیرد، نه صفحه‌ی HTML).
     */
    private static boolean isPanelPage(String uri) {
        return uri.startsWith("/Admin.html") || uri.startsWith("/SalesPanel.html");
    }
}