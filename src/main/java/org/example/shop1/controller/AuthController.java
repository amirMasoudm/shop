package org.example.shop1.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.example.shop1.config.SecurityUtils;
import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.User;

import org.example.shop1.model.enums.Role;
import org.example.shop1.model.reposritory.UserRepository;
import org.example.shop1.model.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository; // اینزرت مستقیم برای سادگی
    private final PasswordEncoder passwordEncoder;

    private SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    public AuthController(AuthService authService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.userRepository = userRepository;

        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/csrf")
    public ResponseEntity<String> getCsrfToken() {
        return ResponseEntity.ok("CSRF Token initialized");
    }
    // مرحله اول لاگین ادمین
    @PostMapping("/admin/login-step1")
    public ResponseEntity<?> adminLoginStep1(
            @RequestBody Map<String,String> payload,
            HttpServletRequest request){
        String username = payload.get("username");
        String password = payload.get("password");

        User admin = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "نام کاربری یا رمز عبور اشتباه است"));

        // فقط ادمین ها مجاز به استفاده از این مسیر هستند
        if (admin.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body("شما اجازه ورود از این بخش را ندارید");
        }

        if (admin.getPassword() == null || !passwordEncoder.matches(password, admin.getPassword())) {
            return ResponseEntity.status(401).body("نام کاربری یا رمز عبور اشتباه است");
        }

        authService.sendOtpCode(admin.getPhoneNumber());


        request.getSession()
                .setAttribute(
                        "PENDING_ADMIN_LOGIN",
                        admin.getUsername()
                );


        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "کد تایید ارسال شد"
                )
        );
    }

    // مرحله دوم لاگین ادمین
    @PostMapping("/admin/login-step2")
    public ResponseEntity<?> adminLoginStep2(@RequestBody Map<String, String> payload,
                                             HttpServletRequest request,
                                             HttpServletResponse response) {
        String username =
                (String) request.getSession()
                        .getAttribute("PENDING_ADMIN_LOGIN");


        if(username == null){

            return ResponseEntity.status(401)
                    .body("جلسه ورود منقضی شده");

        }        String code = payload.get("code");

        User admin =
                userRepository.findByUsername(username)
                        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "کاربر یافت نشد"));

        // بررسی صحت کد OTP
        // (باید یک متد در AuthService بنویسید که فقط کد را چک کند بدون اینکه یوزر جدید بسازد)
        if (!authService.verifyOnlyCode(admin.getPhoneNumber(), code)) {
            return ResponseEntity.badRequest().body("کد وارد شده اشتباه است یا منقضی شده");
        }

        // احراز هویت با موفقیت انجام شد، ثبت در کانتکست
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                admin.getUsername(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + admin.getRole().name()))
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        Map<String,Object> result=new HashMap<>();

        result.put(
                "message",
                "ورود ادمین با موفقیت انجام شد"
        );

        result.put(
                "username",
                admin.getUsername()
        );

        result.put(
                "role",
                admin.getRole()
        );


        return ResponseEntity.ok(result);
    }
    // متد sendOtp بدون تغییر باقی می‌ماند...
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> payload) {
        authService.sendOtpCode(payload.get("phoneNumber"));
        return ResponseEntity.ok("کد ارسال شد");
    }

    // تغییر در متد verifyOtp
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> payload,
                                       HttpServletRequest request,
                                       HttpServletResponse response) { // ریکوئست و ریسپانس را اضافه کنید
        String phone = payload.get("phoneNumber");
        String code = payload.get("code");

        try {
            User user = authService.verifyCodeAndLogin(phone, code);

            // ۱. ایجاد توکن احراز هویت
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    user.getUsername(),
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );

            // ۲. ثبت در کانتکست اسپرینگ
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);

            // ۳. ذخیره در سشن (بسیار مهم: این خط باعث می‌شود سکیوریتی شما را بشناسد)
            securityContextRepository.saveContext(context, request, response);

            boolean isProfileComplete = (user.getFirstName() != null && !user.getFirstName().isEmpty());

            Map<String, Object> resp = new HashMap<>();
            resp.put("user", user);
            resp.put("isProfileComplete", isProfileComplete);

            return ResponseEntity.ok(resp);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // متد جدید برای تکمیل اطلاعات
    @PostMapping("/complete-profile")
    public ResponseEntity<?> completeProfile(@RequestBody Map<String, String> payload) {
        // گرفتن نام کاربری (شماره موبایل) از کانتکست امنیتی اسپرینگ
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return ResponseEntity.status(401).build();

        String phone = auth.getName(); // این شماره قطعا متعلق به فرد درخواست دهنده است
        String firstName = payload.get("firstName");
        String lastName = payload.get("lastName");

        User user = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new RuntimeException("کاربر یافت نشد"));

        user.setFirstName(SecurityUtils.clean(payload.get("firstName")));
        user.setLastName(SecurityUtils.clean(payload.get("lastName")));
        // در متد verify-otp یوزرنیم ست شده، اینجا فقط نام را تکمیل می‌کنیم
        userRepository.save(user);

        return ResponseEntity.ok("پروفایل با موفقیت تکمیل شد");
    }
    // استفاده از RequestMapping تا هم با GET و هم POST کار کند و درگیر CSRF نشود
    @RequestMapping(value = "/api/auth/logout", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {

        // ۱. نابود کردن Security Context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        // ۲. پیدا کردن تمام کوکی‌ها و نابود کردن آن‌ها (خیلی مهم اگر اسم کوکی را نمیدانیم)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                cookie.setValue("");
                cookie.setPath("/"); // حتما پث باید / باشد تا همه جا پاک شود
                cookie.setMaxAge(0);
                response.addCookie(cookie);
            }
        }

        // ۳. نابود کردن سشن سنتی (تیر خلاص)
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }

        return ResponseEntity.ok("Logged out successfully");
    }
}