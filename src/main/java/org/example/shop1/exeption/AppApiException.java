package org.example.shop1.exeption;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * خطای مسیرهای {@code /api/app/v1}. برخلافِ {@link ApiException} که متنِ فارسی برای
 * نمایش دارد، این‌جا بدنه دقیقاً همان JSONِ قرارداد است ({@code {"error":"…", …}})
 * چون دو کلاینتِ جدا (اپ و سایت) رویش منطق می‌سازند، نه رویِ متن.
 */
public class AppApiException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, Object> body;

    public AppApiException(HttpStatus status, Map<String, Object> body) {
        super(String.valueOf(body.get("error")));
        this.status = status;
        this.body = body;
    }

    public HttpStatus getStatus() { return status; }
    public Map<String, Object> getBody() { return body; }

    public static AppApiException invalid(String field) {
        return new AppApiException(HttpStatus.BAD_REQUEST, Map.of("error", "invalid", "field", field));
    }

    public static AppApiException rateLimited(long retryAfterSec) {
        return new AppApiException(HttpStatus.TOO_MANY_REQUESTS,
                Map.of("error", "rate-limited", "retryAfterSec", retryAfterSec));
    }

    public static AppApiException unauthorized() {
        return new AppApiException(HttpStatus.UNAUTHORIZED, Map.of("error", "unauthorized"));
    }
}
