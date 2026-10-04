package org.example.shop1.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.shop1.config.AnalyticsProperties;
import org.example.shop1.model.enums.EventType;
import org.example.shop1.model.service.analytics.AnalyticsContext;
import org.example.shop1.model.service.analytics.UserEventRecorder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * بیکنِ مرورگر — فقط برایِ چیزی که سرور نمی‌بیند: ناوبریِ داخلیِ SPA، فیلتر،
 * مرتب‌سازی، عمقِ مطالعه و سبدِ سمتِ مرورگر.
 * <p>
 * ⚠️ این مسیر {@code permitAll} است و <b>از CSRF معاف</b> (در {@code SecurityConfig})،
 * چون {@code navigator.sendBeacon} توکن نمی‌فرستد. بدونِ آن معافیت همه‌چیز ۴۰۳
 * می‌گرفت و هیچ رویدادی ثبت نمی‌شد.
 * <p>
 * 🔴 <b>به هیچ چیزی که کلاینت می‌فرستد اعتماد نمی‌شود:</b> {@code anonId}،
 * {@code sessionId} و {@code userId} همگی از کوکیِ {@code HttpOnly} و
 * {@code SecurityContext} سمتِ سرور گرفته می‌شوند و فیلدهای متناظر در بدنه — اگر
 * فرستاده شوند — <b>خوانده هم نمی‌شوند</b>. وگرنه هرکسی می‌توانست تاریخچهٔ جعلی برایِ
 * هر کاربری بسازد.
 */
@RestController
@RequestMapping("/api/v1/track")
public class TrackController {

    private final UserEventRecorder recorder;
    private final AnalyticsProperties props;

    /** شمارندهٔ درون‌حافظه‌ای — تک‌نودی هستیم و همین کافی است؛ Redis لازم نیست. */
    private final Map<String, AtomicInteger> hits = new ConcurrentHashMap<>();
    private volatile long windowStartedAt = System.currentTimeMillis();

    public TrackController(UserEventRecorder recorder, AnalyticsProperties props) {
        this.recorder = recorder;
        this.props = props;
    }

    @PostMapping
    public ResponseEntity<Void> track(@RequestBody(required = false) Map<String, Object> body,
                                      HttpServletRequest request) {
        AnalyticsContext ctx = (AnalyticsContext) request.getAttribute(AnalyticsContext.REQUEST_ATTRIBUTE);
        // همیشه ۲۰۴ برمی‌گردد: بیکن پاسخ را نمی‌خواند و خطای ۴xx فقط کنسولِ کاربر را
        // شلوغ می‌کند. مشکلِ سمتِ ما نباید به کاربر نشان داده شود.
        if (ctx == null || body == null) return ResponseEntity.noContent().build();

        if (!allow(ctx.anonId()) || !allow(clientKey(request))) {
            return ResponseEntity.noContent().build();
        }

        Object rawEvents = body.get("events");
        if (!(rawEvents instanceof List<?> events)) return ResponseEntity.noContent().build();

        int accepted = 0;
        for (Object item : events) {
            if (accepted >= props.getBeaconMaxEvents()) break;
            if (!(item instanceof Map<?, ?> raw)) continue;

            EventType type = parseType(str(raw.get("type")));
            if (type == null) continue;          // ناشناخته دور ریخته می‌شود
            if (type == EventType.SESSION_START || type == EventType.IDENTIFY) {
                // این دو فقط سروری معنا دارند؛ اگر از کلاینت پذیرفته شوند، منبعِ ورود
                // و دوختنِ هویت قابلِ جعل می‌شوند.
                continue;
            }

            recorder.record(ctx, type,
                    str(raw.get("path")) != null ? str(raw.get("path")) : request.getHeader("Referer"),
                    str(raw.get("entityType")), str(raw.get("entityId")), str(raw.get("entityName")),
                    raw.get("props") instanceof Map<?, ?> p ? castProps(p) : null,
                    parseInstant(raw.get("at")));
            accepted++;
        }
        return ResponseEntity.noContent().build();
    }

    /** پنجرهٔ یک‌دقیقه‌ایِ ساده؛ با شروعِ پنجرهٔ تازه همهٔ شمارنده‌ها صفر می‌شوند. */
    private boolean allow(String key) {
        if (key == null) return true;
        long now = System.currentTimeMillis();
        if (now - windowStartedAt > 60_000) {
            windowStartedAt = now;
            hits.clear();
        }
        return hits.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet()
                <= props.getRateLimitPerMinute();
    }

    /** کلیدِ نرخ بر اساسِ IP — عمداً ذخیره نمی‌شود، فقط در حافظه و تا پایانِ پنجره. */
    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }

    private EventType parseType(String raw) {
        if (raw == null) return null;
        try {
            return EventType.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Instant parseInstant(Object raw) {
        if (raw == null) return null;
        try {
            if (raw instanceof Number n) return Instant.ofEpochMilli(n.longValue());
            return Instant.parse(String.valueOf(raw));
        } catch (Exception e) {
            return null;   // زمانِ نامعتبر → ثبت‌کننده زمانِ سرور را می‌گذارد
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castProps(Map<?, ?> raw) {
        return (Map<String, Object>) raw;   // سقفِ کلید و حجم در UserEventRecorder اعمال می‌شود
    }

    private String str(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }
}
