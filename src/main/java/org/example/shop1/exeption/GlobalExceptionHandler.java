package org.example.shop1.exeption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.charset.StandardCharsets;

/**
 * مدیریت مرکزی خطاها. پیام قابل فهم به کاربر را به صورت متن ساده (UTF-8) با کد وضعیت
 * صحیح برمی‌گرداند تا فرانت‌اند بتواند دقیقاً علت خطا را به کاربر نشان دهد.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final MediaType TEXT_PLAIN_UTF8 =
            new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<String> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .contentType(TEXT_PLAIN_UTF8)
                .body(ex.getMessage());
    }
}
