package org.example.shop1.exeption;

import org.springframework.http.HttpStatus;

/**
 * استثنای عمومی برنامه که کد وضعیت HTTP و پیام قابل نمایش به کاربر را حمل می‌کند.
 * توسط GlobalExceptionHandler به پاسخ با همان وضعیت و پیام تبدیل می‌شود.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
