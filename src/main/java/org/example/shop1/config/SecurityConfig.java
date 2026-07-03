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

import org.example.shop1.model.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
        http.csrf(csrf -> {
                    CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
                    CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
                    // این خط باعث می‌شود اسپرینگ توکن را بلافاصله بسازد و داخل کوکی مرورگر بگذارد
                    requestHandler.setCsrfRequestAttributeName(null);

                    csrf.csrfTokenRepository(tokenRepository)
                            .csrfTokenRequestHandler(requestHandler);
                })
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // ۱. دسترسی آزاد به صفحات HTML، استاتیک‌ها و صفحه اصلی (لندینگ)
                        .requestMatchers("/", "/CL.html", "/customerPanel.html", "/error").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/fonts/**", "/images/**").permitAll()

                        // ۲. مسیرهای احراز هویت (لاگین، ثبت نام، لاگ‌اوت و وضعیت کاربر)
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/users/api/current-user", "/api/auth/logout").permitAll()

                        // ۳. مسیرهای عمومی سایت (که در فایل CL.html برای نمایش سایت فراخوانی می‌شوند)
                        // فقط متد GET را باز می‌گذاریم تا کسی نتواند محصول جدیدی اضافه کند
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/products/**",
                                "/api/categories/**",
                                "/api/v1/landing-sections/**",
                                "/api/comments/product/**",
                                "/api/settings/**"
                        ).permitAll()

                        // ۴. مسیرهای امنیتی ادمین (اصلاح شده - بدون ** در وسط)
                        .requestMatchers(
                                "/api/users/admin/**",
                                "/api/orders/admin/**",
                                "/api/v1/products/admin/**",
                                "/api/comments/admin/**"
                                , "/Admin.html"
                        ).hasRole("ADMIN")

                        // ۵. هر درخواست دیگری به جز موارد بالا، حتماً نیاز به لاگین دارد (مثل ثبت سفارش، پروفایل و ...)
                        .anyRequest().authenticated()
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