
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

                            // callbackِ درگاهِ ملت هم همان الگو: از سرورِ بانک/مرورگرِ کاربرِ
                            // درحالِ ریدایرکت‌شدن می‌آید، نه یک ریکوئستِ SPAی ما با توکنِ CSRFِ
                            // خودمان. امنیتِ واقعی‌اش با bpVerifyRequestِ سرور-به-سرور تضمین
                            // می‌شود (OrderService.handleMellatCallback)، نه با CSRF/سشن.
                            .ignoringRequestMatchers("/api/orders/mellat-callback")

                            // بیکنِ ردگیری: navigator.sendBeacon توکنِ CSRF نمی‌فرستد و
                            // اصلاً هدرِ دلخواه هم نمی‌تواند بگذارد. بدونِ این معافیت هر
                            // بسته ۴۰۳ می‌گرفت و هیچ رویدادی ثبت نمی‌شد — همان باگی که
                            // در کامیتِ 9a8b36c خوردیم.
                            // خطرِ CSRF اینجا بی‌معنی است: این مسیر هیچ کنشی به‌نامِ کاربر
                            // انجام نمی‌دهد و هویت را هم از کوکیِ HttpOnly و SecurityContext
                            // می‌گیرد، نه از بدنه.
                            .ignoringRequestMatchers("/api/v1/track")
                            ;

                })
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // در SecurityConfig.java بخش authorizeHttpRequests

                .authorizeHttpRequests(auth -> auth
                        // ۱. حتما صفحه خطا را کاملا باز بگذارید
                        .requestMatchers("/error", "/favicon.ico", "/robots.txt", "/sitemap.xml").permitAll()

                        // لینکِ کوتاهِ کارزار — برایِ همه باز است، چون همان لینکی است که
                        // در تلگرام و روی کاتالوگِ چاپی منتشر می‌شود. مقصدش فقط از
                        // دیتابیس خوانده می‌شود و هیچ پارامترِ کوئری‌ای در آن نقش ندارد.
                        .requestMatchers(HttpMethod.GET, "/l/*").permitAll()

                        .requestMatchers("/", "/shop", "/about", "/wimaxnear", "/learn", "/support/link-cal", "/dadehlink", "/privacy", "/app/dadehlink/**", "/shop/product/**", "/shop/category/**", "/shop/course/**", "/blog/**", "/CL.html", "/AdminLogin.html", "/customerPanel.html").permitAll()
                        // پوشهٔ واقعیِ تصاویرِ ثابت `/img/` است؛ `/images/**` هرگز وجود نداشت و
                        // به همین دلیل لوگو در کلِ سایت ۴۰۱ می‌گرفت.
                        // ⚠️ /fragments/** هم باید همینجا باشد: مارک‌آپِ خامِ تبِ محصولات
                        // (products-tab-fragment.html) را products-tab.js با fetch می‌گیرد؛
                        // بدونِ این خط، ۴۰۱ می‌گرفت چون به anyRequest().authenticated() پایین می‌افتاد.
                        .requestMatchers("/css/**", "/js/**", "/fonts/**", "/img/**", "/images/**", "/uploads/**", "/fragments/**", "/tools/**").permitAll()

                        // ⛔ مسیرهای ادمینِ API باید «قبل از» permitAll عمومی بیایند (اولین match برنده است)
                        .requestMatchers("/api/v1/product-redirects/**").hasRole("ADMIN")
                        // ریدایرکتِ آدرس‌های قدیمیِ ایندکس‌شده — خودِ ریدایرکت برای همه باز
                        // است (روی ریشهٔ دامنه و بیرونِ /api/)، ولی مدیریتش فقط ادمین.
                        .requestMatchers("/api/v1/legacy-redirects/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/articles/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/products/admin/**").hasAnyRole("ADMIN", "PRICER")
                        .requestMatchers("/api/v1/rfq/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/settings/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/courses/admin/**").hasRole("ADMIN")
                        // کدِ کالا و ایمپورتِ موجودی — همان دو نقشی که حقِ نوشتنِ محصول
                        // دارند. 🔴 کدِ کالا در پاسخِ عمومیِ محصول دیده می‌شود (عمدی)، ولی
                        // ساخت و تغییرش فقط از همین مسیر ممکن است؛ هیچ مسیرِ عمومی‌ای
                        // نمی‌تواند آن را بنویسد.
                        .requestMatchers("/api/v1/holoo/**").hasAnyRole("ADMIN", "PRICER")

                        // بیکنِ ردگیریِ رفتار — عمومی است چون بازدیدکنندهٔ ناشناس هم باید
                        // ثبت شود. هویت از کوکیِ HttpOnly می‌آید، نه از بدنه.
                        .requestMatchers(HttpMethod.POST, "/api/v1/track").permitAll()

                        // آستانه‌ی RFQ خواندنی و عمومی است (فرانت دکمه را شرطی می‌کند)
                        .requestMatchers(HttpMethod.GET, "/api/v1/settings/rfq-threshold").permitAll()
                        // فاصله‌ی چرخشِ بنر هم خواندنی و عمومی است (CL.html این را می‌خواند)
                        .requestMatchers(HttpMethod.GET, "/api/v1/settings/banner-rotation-seconds").permitAll()

                        // 🔒 نوشتنِ محصول: ADMIN و کارشناسِ ارشد (PRICER) — همان چیزی که
                        // از اول در توضیحِ خودِ نقشِ PRICER نوشته شده بود ولی در قاعده
                        // اعمال نشده بود. مالک صریحاً خواست کارشناسِ ارشد بتواند از پنلِ
                        // فروش محصول اضافه کند و کارتِ ناقص را کامل کند.
                        // خواندن (فهرستِ محصولاتِ پنل) در خطِ /products/admin/** بالاتر
                        // برایِ PRICER از قبل باز بود تا تب را ببیند.
                        // جستجو (POST) عمومی می‌ماند چون فرانت با آن فیلتر می‌کند.
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/search").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/products").hasAnyRole("ADMIN", "PRICER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasAnyRole("ADMIN", "PRICER")
                        // ⚠️ حذف عمداً فقط ADMIN ماند: افزودن و اصلاح برگشت‌پذیرند، حذف نه.
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("ADMIN")

                        // بنرهایِ خانه — عمداً الگویِ landing-sections را کپی نکردم: آن مسیر
                        // فعلاً کاملاً permitAll است (حتی POST/DELETE)، یعنی هرکسِ ناشناس هم
                        // می‌تواند سکشن بسازد/حذف کند. اینجا کدِ تازه است، پس درست نوشته می‌شود:
                        // فقط خواندنِ فعال‌ها عمومی است، نوشتن فقط ADMIN.
                        .requestMatchers(HttpMethod.GET, "/api/v1/banners/active").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/banners/admin").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/banners").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/banners/**").hasRole("ADMIN")

                        // آرشیوِ تصویریِ آموزش — همان الگو: فقط خواندنِ فعال‌ها عمومی، نوشتن فقط ADMIN.
                        .requestMatchers(HttpMethod.GET, "/api/v1/education-archive/active").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/education-archive/admin").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/education-archive").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/education-archive/**").hasRole("ADMIN")

                        // پنجره‌هایِ ردیفیِ بنرِ تصویری — همان الگو: فقط خواندنِ فعال‌ها عمومی، نوشتن فقط ADMIN.
                        .requestMatchers(HttpMethod.GET, "/api/v1/image-row-banners/active").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/image-row-banners/admin").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/image-row-banners/admin").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/image-row-banners/**").hasRole("ADMIN")

                        // دوره‌های آموزشی — خواندنِ عمومی (فهرست + جزئیاتِ یک دوره)، نوشتن فقط ADMIN
                        // (بالاتر با /api/v1/courses/admin/** پوشش داده شد).
                        .requestMatchers(HttpMethod.GET, "/api/v1/courses/active", "/api/v1/courses/single/**").permitAll()

                        // دسته‌بندی‌ها — قبلاً کاملاً permitAll بود (حتی POST/PUT/DELETE، یعنی هرکسِ
                        // ناشناس می‌توانست دسته بسازد/حذف کند)؛ همان الگویِ بالا: فقط خواندن عمومی،
                        // نوشتن فقط ADMIN.
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")

                        // ۲. مسیرهای عمومی API را با دقت بیشتر باز کنید (حذف HttpMethod.GET برای تست اگر جواب نداد)
                        .requestMatchers(
                                "/api/v1/products/**",
                                "/api/v1/landing-sections/**", // اینجا در لاگ خطا میداد
                                "/api/comments/product/**",
                                "/api/v1/articles/**",
                                "/api/settings/**"
                        ).permitAll()

                        // میزِ کارِ قیمت‌گذاری — محافظت در سطحِ مسیر، نه فقط پنهان‌کردنِ دکمه در UI.
                        // ترتیب مهم است: قواعدِ خاص‌ترِ نوشتن/لاگ/خواندنِ-فقط‌مشاهده باید قبل از
                        // قاعده‌ی کلیِ خواندن/نوشتن بیایند.
                        .requestMatchers(HttpMethod.GET, "/api/v1/pricing/logs").hasRole("ADMIN")
                        // کارشناسِ فروش (SUPPORT) فقط مشاهده دارد؛ هیچ POSTی برایش مجاز نیست
                        // (نه batch، نه marketplace) — همان سه GETِ زیر برایِ نمایشِ
                        // میزِ کار در حالتِ فقط-خواندنی کافی است.
                        .requestMatchers(HttpMethod.GET, "/api/v1/pricing/rows", "/api/v1/pricing/capabilities", "/api/v1/pricing/logs/products")
                                .hasAnyRole("ADMIN", "PRICER", "SALES", "SUPPORT")
                        // 🔒 نوشتن در میزِ کار: ADMIN، کارشناسِ ارشد (PRICER)، و کارشناسِ
                        // فروش و قیمت‌گذاری (SALES).
                        // ⚠️ تاریخچهٔ تصمیم: اول «فقط ادمین» بود، بعد کارشناسِ ارشد اضافه شد،
                        // و در ۲۰۲۶-۰۹-۲۰ مالک صریحاً گفت «تمامِ کارشناس‌ها، نه فقط ارشد».
                        // SUPPORT (کارشناسِ پشتیبانی) عمداً بیرون ماند: شغلِ دیگری است و
                        // تا حالا فقط مشاهده داشته؛ بازکردنش باید تصمیمِ جداگانه باشد.
                        // مرز همچنان سمتِ سرور است، نه فقط پنهان‌کردنِ دکمه در UI.
                        .requestMatchers(HttpMethod.POST, "/api/v1/pricing/batch").hasAnyRole("ADMIN", "PRICER", "SALES")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/pricing/reorder").hasAnyRole("ADMIN", "PRICER", "SALES")
                        .requestMatchers(HttpMethod.POST, "/api/v1/pricing/marketplace/**").hasAnyRole("ADMIN", "PRICER", "SALES")
                        .requestMatchers("/api/v1/pricing/**").hasAnyRole("ADMIN", "PRICER", "SALES")

                        // Torob Product API — ترب خودش با JWTِ ed25519 احراز می‌شود، نه سشنِ ما.
                        // بدونِ این خط، قاعده‌ی anyRequest().authenticated() پایین بلاکش می‌کند
                        // (همان کلاسِ باگی که قبلاً روی /img/** خوردیم).
                        .requestMatchers(HttpMethod.POST, "/torob_api/v3/products").permitAll()

                        // callbackِ درگاهِ ملت — سرورِ بانک سشنِ ما را ندارد؛ بدونِ این خط
                        // anyRequest().authenticated() پایین ۴۰۱ می‌داد و تراکنشِ واقعی گم می‌شد.
                        .requestMatchers(HttpMethod.POST, "/api/orders/mellat-callback").permitAll()

                        // چتِ پشتیبانی — همه‌چیزش احرازِ هویت می‌خواهد. تفکیکِ مشتری/کارشناس
                        // در سطحِ مسیر ممکن نیست (هر دو از یک اندپوینت استفاده می‌کنند)، پس
                        // ChatService.assertMember در هر فراخوانی عضویت را چک می‌کند.
                        // ⚠️ ساعتِ کاری فقط خواندنی و عمومی است تا حبابِ چت بتواند پیشِ ورود
                        //    هم بگوید چه ساعتی فعال می‌شود؛ تغییرش زیرِ /settings/admin/** است.
                        .requestMatchers(HttpMethod.GET, "/api/v1/settings/chat-hours").permitAll()
                        .requestMatchers("/api/v1/chat/**").authenticated()
                        // هندشیکِ وب‌سوکت؛ ChatHandshakeInterceptor مستقلاً هم سشن را چک می‌کند.
                        .requestMatchers("/ws/**").authenticated()

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
                        // SUPPORT (کارشناسِ فروش، فقط‌مشاهده) عمداً فقط اینجا اضافه شد، نه
                        // به /Admin.html (خطِ ۱۶۹).
                        .requestMatchers("/SalesPanel.html").hasAnyRole("ADMIN", "PRICER", "SALES", "SUPPORT")

                        // ۴. مسیرهای ادمین
                        .requestMatchers(
                                "/api/users/admin/**",
                                "/api/orders/admin/**",
                                "/api/comments/admin/**",
                                // 🔴 تبِ رفتارِ کاربران: «سفرِ کاربر»، خروجی و فایل‌های
                                // آرشیو همه دادهٔ شخصیِ مشتریانِ واقعی‌اند. برخلافِ تبِ
                                // پشتیبانیِ چت که عمداً برای هر چهار نقش باز است، این
                                // یکی فقط ADMIN — نه PRICER، نه SALES، نه SUPPORT.
                                "/api/v1/analytics/**",
                                // دفترِ کارزارها و لینک‌ساز — هزینه و برنامهٔ بازاریابی است.
                                "/api/v1/campaigns/**",
                                // ثبت‌نام‌های اپ و ابزارِ سایت: نام و شمارهٔ کاملِ مشتری.
                                "/api/v1/app-registrations/**",
                                // دادهٔ فنیِ رادیویی (ویرایشِ دستی و ابزارِ واردکردن)
                                "/api/v1/rf-specs/**"
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
                            } else if (isPublicSurfaceGet(request)) {
                                // 🔴 GETِ ناشناس روی مسیرِ ناشناخته باید ۴۰۴ بگیرد، نه ۴۰۱.
                                // بعد از سوییچِ دامنه، هر آدرسِ قدیمیِ نگاشت‌نشده‌ای که گوگل
                                // می‌شناسد به همین‌جا می‌خورد؛ ۴۰۱ برای خزنده مبهم است و
                                // بارها دوباره امتحانش می‌کند، در حالی که ۴۰۴ تکلیف را
                                // روشن می‌کند. برای مهاجم هم ۴۰۴ کم‌تر افشا می‌کند، چون
                                // وجود/نبودِ مسیر را لو نمی‌دهد.
                                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Not Found");
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
                // سشن‌ها در مانگو ذخیره می‌شوند (spring-session-data-mongodb)؛ باطل‌کردنِ
                // سشنِ یک کاربر بعد از تغییرِ نقشش در UserService.changeUserRole انجام
                // می‌شود (حذفِ مستقیم از همان مخزن). SessionRegistryِ درون‌حافظه‌ای اینجا
                // عمداً استفاده نشده چون با هر ری‌استارت خالی می‌شود و سشنِ مانگو را
                // اصلاً لمس نمی‌کند.
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

    /**
     * آیا این یک درخواستِ خواندنِ ناشناس روی سطحِ عمومیِ سایت است؟
     * <p>
     * {@code /api/**} عمداً بیرون است: کلاینتِ API باید ۴۰۱ ببیند تا بفهمد باید وارد
     * شود؛ ۴۰۴ آن‌جا فقط دیباگ را سخت می‌کند. فایل‌های {@code .html} هم بیرون‌اند تا
     * رفتارِ صفحه‌های پنل دست‌نخورده بماند.
     */
    private static boolean isPublicSurfaceGet(jakarta.servlet.http.HttpServletRequest request) {
        String method = request.getMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) return false;
        String uri = request.getRequestURI();
        return !uri.startsWith("/api/") && !uri.endsWith(".html");
    }
}