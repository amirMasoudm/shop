package org.example.shop1.model.service;

import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.dto.SmsParameter;
import org.example.shop1.model.dto.SmsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    // مقادیر از application.properties / متغیرهای محیطی خوانده می‌شوند (نباید در سورس هاردکد شوند)
    @Value("${sms.api-key:}")
    private String apiKey;

    @Value("${sms.template-id:0}")
    private int templateId;

    // قالبِ «دوباره موجود شد» (متغیرِ PRODUCT) — جدا از قالبِ OTP
    @Value("${sms.stock-template-id:0}")
    private int stockTemplateId;

    // SMS.ir در اندپوینتِ verify هر مقدارِ پارامتر را حداکثر ۲۵ کاراکتر می‌پذیرد (خطای status 114 برای بیشتر).
    private static final int SMS_PARAM_MAX_LEN = 25;

    @Value("${sms.url:https://api.sms.ir/v1/send/verify}")
    private String url;

    private final RestTemplate restTemplate;

    public SmsService() {
        this.restTemplate = new RestTemplate();
    }

    public void sendOtp(String mobile, String code) {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("SMS API key تنظیم نشده است (sms.api-key)");
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "سرویس پیامک در دسترس نیست");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", apiKey);

        // نام متغیر در قالب شما. مثلا اگر در قالب نوشتید "کد: #CODE#" اینجا باید "CODE" باشد
        SmsParameter param = new SmsParameter("CODE", code);

        SmsRequest requestBody = new SmsRequest(mobile, templateId, Collections.singletonList(param));

        HttpEntity<SmsRequest> entity = new HttpEntity<>(requestBody, headers);

        try {
            restTemplate.postForObject(url, entity, String.class);
            // نه کد OTP و نه کلید API نباید در لاگ ثبت شوند
            log.info("پیامک تایید برای شماره {} ارسال شد", mobile);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("خطا در ارسال پیامک به {}: {}", mobile, e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "خطا در ارسال پیامک، لطفاً بعداً تلاش کنید");
        }
    }

    /**
     * پیامکِ «محصول دوباره موجود شد» (قالبِ SMS.ir با متغیرِ PRODUCT).
     * در صورتِ خطا استثنا پرتاب می‌کند تا فراخوان (تریگرِ async) بتواند رکورد را notified نکند و بعداً دوباره تلاش کند.
     */
    public void sendBackInStockNotification(String mobile, String productName) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("SMS API key تنظیم نشده؛ اطلاع‌رسانیِ موجودی برای {} ارسال نشد", mobile);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "سرویس پیامک در دسترس نیست");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", apiKey);

        // نامِ متغیرِ قالب دقیقاً PRODUCT است (مثلِ CODE در sendOtp).
        // نام‌های بلندِ محصول (مثلِ مدل‌های شبکه) از سقفِ ۲۵ کاراکتریِ SMS.ir رد می‌شوند → کوتاه می‌کنیم.
        SmsParameter param = new SmsParameter("PRODUCT", truncateForSms(productName));
        SmsRequest requestBody = new SmsRequest(mobile, stockTemplateId, Collections.singletonList(param));
        HttpEntity<SmsRequest> entity = new HttpEntity<>(requestBody, headers);

        try {
            restTemplate.postForObject(url, entity, String.class);
            log.info("پیامکِ موجودشدن برای شماره {} ارسال شد", mobile);
        } catch (Exception e) {
            log.error("خطا در ارسال پیامکِ موجودی به {}: {}", mobile, e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "خطا در ارسال پیامکِ موجودی");
        }
    }

    // کوتاه‌کردنِ مقدارِ پارامتر تا سقفِ مجازِ SMS.ir (۲۵ کاراکتر)؛ اگر بریده شد «…» می‌گذارد.
    private String truncateForSms(String value) {
        if (value == null) return "";
        String v = value.trim();
        if (v.length() <= SMS_PARAM_MAX_LEN) return v;
        return v.substring(0, SMS_PARAM_MAX_LEN - 1) + "…";
    }
}