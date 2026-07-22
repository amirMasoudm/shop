
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
                            ;

                })
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // در SecurityConfig.java بخش authorizeHttpRequests

                .authorizeHttpRequests(auth -> auth
                        // ۱. حتما صفحه خطا را کاملا باز بگذارید
                        .requestMatchers("/error", "/favicon.ico", "/robots.txt", "/sitemap.xml").permitAll()

                        .requestMatchers("/", "/product/**", "/category/**", "/blog/**", "/CL.html", "/AdminLogin.html", "/customerPanel.html").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/fonts/**", "/images/**", "/uploads/**").permitAll()

                        // ⛔ مسیرهای ادمینِ API باید «قبل از» permitAll عمومی بیایند (اولین match برنده است)
                        .requestMatchers("/api/v1/articles/admin/**").hasRole("ADMIN")

                        // ۲. مسیرهای عمومی API را با دقت بیشتر باز کنید (حذف HttpMethod.GET برای تست اگر جواب نداد)
                        .requestMatchers(
                                "/api/v1/products/**",
                                "/api/categories/**",
                                "/api/v1/landing-sections/**", // اینجا در لاگ خطا میداد
                                "/api/comments/product/**",
                                "/api/v1/articles/**",
                                "/api/settings/**"
                        ).permitAll()

                        // ۳. باقی مسیرها
                        .requestMatchers("/api/auth/**").permitAll()

                        // ۴. مسیرهای ادمین
                        .requestMatchers(
                                "/Admin.html",
                                "/api/users/admin/**",
                                "/api/orders/admin/**",
                                "/api/v1/products/admin/**",
                                "/api/comments/admin/**"
                        ).hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                // ۴. این بخش را اضافه کن: اگر کاربر لاگین نبود و خواست وارد Admin.html شود، به AdminLogin.html هدایت شود
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (request.getRequestURI().startsWith("/Admin.html")) {
                                response.sendRedirect("/AdminLogin.html");
                            } else {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                            }
                        })
                        // این بخش جدید است: وقتی شخص لاگین کرده اما ادمین نیست (ROLE_USER دارد)
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (request.getRequestURI().startsWith("/Admin.html")) {
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
}