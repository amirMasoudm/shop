package org.example.shop1.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.shop1.config.AppTokenAuthFilter.AppTokenAuthentication;
import org.example.shop1.exeption.AppApiException;
import org.example.shop1.model.entity.AppToken;
import org.example.shop1.model.entity.User;
import org.example.shop1.model.enums.EventType;
import org.example.shop1.model.enums.Role;
import org.example.shop1.model.service.AppAuthService;
import org.example.shop1.model.service.analytics.UserEventRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * مسیرهای قراردادِ {@code docs/dadehlink-api-contract.md} — نامِ مسیر، بدنه و کدِ پاسخ
 * عیناً همان؛ اپِ اندرویدی هم‌زمان رویش ساخته می‌شود.
 * <p>
 * بدنه‌ها عمداً به‌صورتِ رشتهٔ خام خوانده می‌شوند: قرارداد سقفِ ۴ کیلوبایت دارد و
 * خطای اعتبارسنجی باید {@code {"error":"invalid","field":…}} باشد، نه صفحهٔ خطای
 * پیش‌فرضِ اسپرینگ.
 */
@RestController
@RequestMapping("/api/app/v1")
public class AppAuthController {

    private static final Logger log = LoggerFactory.getLogger(AppAuthController.class);
    private static final int MAX_BODY_BYTES = 4096;

    private final AppAuthService appAuth;
    private final ObjectMapper json;
    private final UserEventRecorder analytics;
    private final SecurityContextRepository sessionContext = new HttpSessionSecurityContextRepository();

    public AppAuthController(AppAuthService appAuth, ObjectMapper json, UserEventRecorder analytics) {
        this.appAuth = appAuth;
        this.json = json;
        this.analytics = analytics;
    }

    // ۱
    @PostMapping(value = "/auth/request-code", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> requestCode(@RequestBody(required = false) String raw, HttpServletRequest request) {
        JsonNode b = parse(raw);
        long resend = appAuth.requestCode(new AppAuthService.RequestCodeInput(
                text(b, "name"), text(b, "phone"), text(b, "company"), text(b, "installationId"),
                text(b, "client"), text(b, "appVersion"), text(b, "consentVersion")), clientIp(request));
        return ResponseEntity.ok(Map.of("status", "code-sent", "resendAfterSec", resend));
    }

    // ۲
    @PostMapping(value = "/auth/verify", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> verify(@RequestBody(required = false) String raw,
                                    HttpServletRequest request, HttpServletResponse response) {
        JsonNode b = parse(raw);
        AppAuthService.Issued issued = appAuth.verify(text(b, "phone"), text(b, "code"),
                text(b, "installationId"), clientIp(request));
        if ("web".equals(issued.client())) {
            loginSite(issued.user(), issued.userCreated(), request, response);
        }
        return ResponseEntity.ok(registered(issued));
    }

    // ۳
    @GetMapping("/me")
    public ResponseEntity<?> me() {
        AppToken t = currentToken();
        User u = appAuth.userOf(t).orElseThrow(AppApiException::unauthorized);
        appAuth.touch(t);
        return ResponseEntity.ok(Map.of("user", userBody(u)));
    }

    // ۴
    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout() {
        appAuth.revoke(currentToken());
        return ResponseEntity.noContent().build();
    }

    // ۵ — فقط سایت: کاربرِ ازقبل‌واردشده بی‌پیامک توکن می‌گیرد.
    @PostMapping(value = "/auth/session-token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> sessionToken(@RequestBody(required = false) String raw, HttpServletRequest request) {
        Authentication site = siteAuthentication(request);
        if (site == null) throw AppApiException.unauthorized();
        JsonNode b = parse(raw);
        AppAuthService.Issued issued = appAuth.sessionToken(site.getName(), text(b, "installationId"),
                clientIp(request));
        return ResponseEntity.ok(registered(issued));
    }

    @ExceptionHandler(AppApiException.class)
    public ResponseEntity<Map<String, Object>> onAppError(AppApiException e) {
        return ResponseEntity.status(e.getStatus()).body(e.getBody());
    }

    // ==========================================================

    private Map<String, Object> registered(AppAuthService.Issued issued) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "registered");
        body.put("token", issued.token());
        body.put("user", userBody(issued.user()));
        return body;
    }

    private static Map<String, Object> userBody(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", AppAuthService.displayName(u));
        m.put("phone", AppAuthService.maskPhone(u.getPhoneNumber()));
        return m;
    }

    private static AppToken currentToken() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a instanceof AppTokenAuthentication app) return app.getToken();
        // سشنِ سایت این‌جا هویت حساب نمی‌شود — فقط توکن.
        throw AppApiException.unauthorized();
    }

    /** سشنِ خودِ سایت، مستقل از هر سرآیندِ Bearer که شاید هم‌زمان آمده باشد. */
    private Authentication siteAuthentication(HttpServletRequest request) {
        SecurityContext ctx = sessionContext.loadDeferredContext(request).get();
        Authentication a = ctx == null ? null : ctx.getAuthentication();
        if (a == null || !a.isAuthenticated() || a instanceof AnonymousAuthenticationToken
                || a instanceof AppTokenAuthentication) {
            return null;
        }
        return a;
    }

    /**
     * {@code client=web}: کاربر همان لحظه در خودِ سایت هم وارد می‌شود — همان سشنی که
     * {@code AuthController.verifyOtp} می‌سازد.
     * <p>
     * 🔴 فقط برای نقشِ USER. کارکنان (ADMIN، PRICER، …) با شماره و پیامکِ تنها وارد
     * نمی‌شوند؛ ورودشان دومرحله‌ای (رمز + پیامک) است و این مسیر نباید میان‌بُرِ آن
     * باشد. برای آن‌ها فقط توکنِ ابزار صادر می‌شود.
     */
    private void loginSite(User user, boolean created, HttpServletRequest request, HttpServletResponse response) {
        if (user.getRole() != Role.USER) {
            log.info("ورودِ سایت از مسیرِ اپ برای نقشِ {} انجام نشد (فقط توکنِ ابزار).", user.getRole());
            return;
        }
        if (request.getSession(false) != null) request.changeSessionId();   // تثبیتِ سشن
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        sessionContext.saveContext(ctx, request, response);
        try {
            analytics.identify(user.getId());
            analytics.record(created ? EventType.REGISTER : EventType.LOGIN, "USER", user.getId(), null);
        } catch (RuntimeException e) {
            log.debug("ثبتِ رویدادِ ورود ناموفق بود: {}", e.toString());
        }
    }

    private JsonNode parse(String raw) {
        if (raw == null || raw.isBlank()) throw AppApiException.invalid("body");
        if (raw.getBytes(StandardCharsets.UTF_8).length > MAX_BODY_BYTES) throw AppApiException.invalid("body");
        try {
            JsonNode n = json.readTree(raw);
            if (n == null || !n.isObject()) throw AppApiException.invalid("body");
            return n;
        } catch (AppApiException e) {
            throw e;
        } catch (Exception e) {
            throw AppApiException.invalid("body");
        }
    }

    /** فقط مقدارِ رشته‌ای؛ عدد یا شیء به‌جای رشته یعنی ورودیِ خراب، نه تبدیلِ بی‌صدا. */
    private static String text(JsonNode b, String field) {
        JsonNode v = b.get(field);
        if (v == null || v.isNull()) return null;
        if (!v.isTextual()) throw AppApiException.invalid(field);
        return v.asText();
    }

    /** X-Real-IP را nginx از خودِ اتصال می‌نشاند و کلاینت نمی‌تواند جعلش کند؛ X-Forwarded-For را می‌تواند. */
    static String clientIp(HttpServletRequest request) {
        String real = request.getHeader("X-Real-IP");
        return real != null && !real.isBlank() ? real.trim() : request.getRemoteAddr();
    }
}
