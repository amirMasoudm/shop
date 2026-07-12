//package org.example.shop1.config;
//
//import org.example.shop1.model.service.CustomUserDetailsService;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpMethod;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//
//import java.util.List;
//
//@Configuration
//@EnableWebSecurity
//public class SecurityConfig {
//
//    private final CustomUserDetailsService userDetailsService;
//
//    public SecurityConfig(CustomUserDetailsService userDetailsService) {
//        this.userDetailsService = userDetailsService;
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    @Bean
//    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
//        return authConfig.getAuthenticationManager();
//    }
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//                .cors(Customizer.withDefaults()) // استفاده از corsConfigurationSource زیر
//                .csrf(csrf -> csrf.disable())
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/api/auth/**", "/users/api/current-user", "/api/customers/**", "/api/orders/**").authenticated()
//                        .requestMatchers("/uploads/**", "/home.html", "/customerPanel.html", "/**/*.js", "/**/*.css", "/**/*.png", "/**/*.jpg").permitAll()
//                        .anyRequest().authenticated()
//                )
//                .formLogin(form -> form
//                        .loginPage("/phoneAuth.html") // یا هر صفحه لاگین شما
//                        .permitAll()
//                )
//                .logout(logout -> logout
//                        .logoutSuccessUrl("/phoneAuth.html")
//                        .permitAll()
//                );
//
//        return http.build();
//    }
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//
//        // به جای allowedOrigins("*") از این استفاده کنید:
//        configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "https://localhost:*"));
//        // یا اگر دامنه خاصی دارید:
//        // configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5500"));
//
//        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//        configuration.setAllowedHeaders(List.of("*"));
//        configuration.setAllowCredentials(true); // مهم: برای سشن و کوکی
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//        return source;
//    }
//}
//3-
package org.example.shop1.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
                            .ignoringRequestMatchers("/api/orders/**");

                })
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // در SecurityConfig.java بخش authorizeHttpRequests

                .authorizeHttpRequests(auth -> auth
                        // ۱. حتما صفحه خطا را کاملا باز بگذارید
                        .requestMatchers("/error", "/favicon.ico").permitAll()

                        .requestMatchers("/", "/CL.html", "/AdminLogin.html", "/customerPanel.html").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/fonts/**", "/images/**").permitAll()

                        // ۲. مسیرهای عمومی API را با دقت بیشتر باز کنید (حذف HttpMethod.GET برای تست اگر جواب نداد)
                        .requestMatchers(
                                "/api/v1/products/**",
                                "/api/categories/**",
                                "/api/v1/landing-sections/**", // اینجا در لاگ خطا میداد
                                "/api/comments/product/**",
                                "/api/settings/**",
                                "/api/auth/csrf"
                        ).permitAll()

                        // ۳. باقی مسیرها
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/users/api/current-user", "/api/auth/logout").permitAll()

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
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:63342", "http://localhost:8080"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
//۲
//package org.example.shop1.config;
//
//import org.example.shop1.model.service.CustomUserDetailsService;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpMethod; // اضافه کن
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

//import java.util.List;

//@Configuration
//public class SecurityConfig {
//
//    private final CustomUserDetailsService userDetailsService;
//
//    public SecurityConfig(CustomUserDetailsService userDetailsService) {
//        this.userDetailsService = userDetailsService;
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    @Bean
//    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
//        return http.getSharedObject(AuthenticationManagerBuilder.class)
//                .userDetailsService(userDetailsService)
//                .passwordEncoder(passwordEncoder())
//                .and()
//                .build();
//    }
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//                .csrf(csrf -> csrf.disable()) // برای محیط توسعه غیرفعال است
//                .authorizeHttpRequests(auth -> auth
//                        // *** تغییر: OPTIONS رو برای همه مسیرها permitAll کن تا CORS preflight کار کنه ***
//                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
//                        // دسترسی‌های عمومی (بدون لاگین)
//                        .requestMatchers(
//                                "/api/customers", "/userAuth.html", "/users/signup", "/home.html",
//                                "/category.html", "/perform_login", "/uploads/**",
//                                "/api/v1/files/**"
//                                ,"/api/auth/**" // <<< این خط را حتما اضافه کنید
//                                // *** تغییر: برای تست، همه APIها رو permitAll کن (بعدا محدود کن) ***
//                                , "/api/**"  // این شامل /api/sales/online/** می‌شه
//                        ).permitAll()
//                        // دسترسی‌های نیازمند احراز هویت
//                        .requestMatchers("/api/current-user").authenticated()
//                        .anyRequest().authenticated()
//                )
//                .formLogin(form -> form
//                        .loginPage("/userAuth.html")
//                        .loginProcessingUrl("/perform_login")
//                        .defaultSuccessUrl("/users/home", true)
//                        .failureUrl("/userAuth.html?error=true")
//                        .permitAll()
//                )
//                .logout(logout -> logout
//                        .logoutUrl("/logout")
//                        .logoutSuccessUrl("/userAuth.html?logout=true")
//                        .permitAll()
//                );
//        return http.build();
//    }
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//        // *** تغییر: برای تست، "*" بگذار (همه اورجین‌ها) – بعدا محدود کن ***
//        configuration.setAllowedOrigins(List.of("*")); // یا لیست خاص: List.of("http://localhost:5173", "http://localhost:63342", "http://localhost:3000")
//        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//        configuration.setAllowedHeaders(List.of("*"));
//        configuration.setAllowCredentials(true);
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//        return source;
//    }
//}

//۱
//package org.example.shop1.config;
//
//import org.example.shop1.model.service.CustomUserDetailsService;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//
//import java.util.List;
//
//@Configuration
//public class SecurityConfig {
//
//    private final CustomUserDetailsService userDetailsService;
//
//    public SecurityConfig(CustomUserDetailsService userDetailsService) {
//        this.userDetailsService = userDetailsService;
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    @Bean
//    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
//        return http.getSharedObject(AuthenticationManagerBuilder.class)
//                .userDetailsService(userDetailsService)
//                .passwordEncoder(passwordEncoder())
//                .and()
//                .build();
//    }
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//                .csrf(csrf -> csrf.disable()) // برای محیط توسعه غیرفعال است
//                .authorizeHttpRequests(auth -> auth
//                        // دسترسی‌های عمومی (بدون لاگین)
//                        .requestMatchers(
//                                "/userAuth.html",
//                                "/users/signup",
//                                "/home.html",
//                                "/category.html",
//                                "/perform_login",
//                                "/uploads/**",       // دسترسی به تصاویر آپلود شده
//                                "/api/v1/files/**"   // اجازه آپلود فایل (می‌توانید محدود کنید)
//                        ).permitAll()
//
//                        // دسترسی‌های نیازمند احراز هویت
//                        .requestMatchers("/api/current-user").authenticated()
//                        .requestMatchers("/api/**").hasRole("USER") // یا hasAnyRole("USER", "ADMIN")
//                        .anyRequest().authenticated()
//                )
//                .formLogin(form -> form
//                        .loginPage("/userAuth.html")
//                        .loginProcessingUrl("/perform_login")
//                        .defaultSuccessUrl("/users/home", true)
//                        .failureUrl("/userAuth.html?error=true")
//                        .permitAll()
//                )
//                .logout(logout -> logout
//                        .logoutUrl("/logout")
//                        .logoutSuccessUrl("/userAuth.html?logout=true")
//                        .permitAll()
//                );
//
//        return http.build();
//    }
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//        // پورت‌های فرانت‌اند خود را اینجا اضافه کنید
//        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:63342"));
//        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//        configuration.setAllowedHeaders(List.of("*"));
//        configuration.setAllowCredentials(true);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//        return source;
//    }
//}