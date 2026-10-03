package org.example.shop1.controller;

import org.example.shop1.model.entity.AppRegistration;
import org.example.shop1.model.entity.AppToken;
import org.example.shop1.model.reposritory.AppRegistrationRepository;
import org.example.shop1.model.reposritory.AppTokenRepository;
import org.example.shop1.model.service.AppAuthService;
import org.example.shop1.model.service.payment.JalaliDateUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * فهرستِ ثبت‌نام‌های اپ و ابزارِ سایت — فقط ADMIN (در {@code SecurityConfig}).
 * <p>
 * شمارهٔ کامل این‌جا نشان داده می‌شود چون کارفرما همین را برای پیگیری می‌خواهد؛ به
 * همین دلیل هیچ نقشِ دیگری (PRICER، SALES، SUPPORT) به آن راه ندارد.
 */
@RestController
@RequestMapping("/api/v1/app-registrations")
public class AppRegistrationAdminController {

    private static final ZoneId TEHRAN = ZoneId.of("Asia/Tehran");
    private static final int MAX_ROWS = 2000;

    private final AppRegistrationRepository registrations;
    private final AppTokenRepository tokens;
    private final AppAuthService appAuth;

    public AppRegistrationAdminController(AppRegistrationRepository registrations, AppTokenRepository tokens,
                                          AppAuthService appAuth) {
        this.registrations = registrations;
        this.tokens = tokens;
        this.appAuth = appAuth;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        List<AppRegistration> rows = newestFirst();
        Map<String, AppToken> byId = new HashMap<>();
        tokens.findAllById(rows.stream().map(AppRegistration::getTokenId).filter(id -> id != null).toList())
                .forEach(t -> byId.put(t.getId(), t));

        List<Map<String, Object>> out = new ArrayList<>(rows.size());
        for (AppRegistration r : rows) {
            AppToken t = r.getTokenId() == null ? null : byId.get(r.getTokenId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("name", r.getName());
            m.put("phone", r.getPhone());
            m.put("company", r.getCompany());
            m.put("client", r.getClient());
            m.put("appVersion", r.getAppVersion());
            m.put("method", r.getMethod());
            m.put("createdAt", r.getCreatedAt());
            m.put("dateFa", jalali(r.getCreatedAt()));
            m.put("tokenActive", t != null && t.isActive());
            m.put("lastUsedAt", t == null ? null : t.getLastUsedAt());
            out.add(m);
        }
        return out;
    }

    /** ابطالِ توکنِ همان ثبت‌نام. کاربر در تعاملِ بعدیِ ابزار دوباره ثبت می‌کند. */
    @PostMapping("/{id}/revoke")
    public ResponseEntity<?> revoke(@PathVariable String id) {
        AppRegistration r = registrations.findById(id).orElse(null);
        if (r == null || r.getTokenId() == null) return ResponseEntity.notFound().build();
        AppToken t = tokens.findById(r.getTokenId()).orElse(null);
        if (t == null) return ResponseEntity.notFound().build();
        if (t.isActive()) appAuth.revoke(t);
        return ResponseEntity.ok(Map.of("tokenActive", false));
    }

    @GetMapping(value = "/export.csv")
    public ResponseEntity<byte[]> csv() {
        StringBuilder sb = new StringBuilder("﻿");   // BOM تا اکسل فارسی را درست باز کند
        sb.append("تاریخ,نام,موبایل,شرکت,کلاینت,نسخه,روش\r\n");
        for (AppRegistration r : newestFirst()) {
            sb.append(cell(jalali(r.getCreatedAt()))).append(',')
                    .append(cell(r.getName())).append(',')
                    .append(cell(r.getPhone())).append(',')
                    .append(cell(r.getCompany())).append(',')
                    .append(cell(r.getClient())).append(',')
                    .append(cell(r.getAppVersion())).append(',')
                    .append(cell(r.getMethod())).append("\r\n");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"app-registrations.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private List<AppRegistration> newestFirst() {
        return registrations.findAll(PageRequest.of(0, MAX_ROWS, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent();
    }

    /** {@code ۱۴۰۵/۰۷/۱۰ ۱۴:۳۵} به وقتِ تهران. */
    static String jalali(Instant at) {
        if (at == null) return "";
        LocalDateTime t = LocalDateTime.ofInstant(at, TEHRAN);
        int[] j = JalaliDateUtil.toJalali(t.getYear(), t.getMonthValue(), t.getDayOfMonth());
        return String.format("%04d/%02d/%02d %02d:%02d", j[0], j[1], j[2], t.getHour(), t.getMinute());
    }

    /**
     * سلولِ CSV: نقل‌قول برای ویرگول و خطِ تازه، و خنثی‌کردنِ فرمول — نامی که کاربر
     * با {@code =} یا {@code +} شروع کند، در اکسل اجرا نمی‌شود.
     */
    static String cell(String v) {
        if (v == null) return "";
        String s = v;
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
