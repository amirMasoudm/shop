package org.example.shop1.model.service;

import org.example.shop1.model.dto.SmsParameter;
import org.example.shop1.model.dto.SmsRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Service
public class SmsService {

    // این مقادیر را از پنل SMS.ir بردارید
    private static final String API_KEY = "gGmLQba04xo6e3TiRQMccJfXgHMQmpays7XzBgypf8xJw3IX";
    private static final int TEMPLATE_ID = 114191; // شناسه قالبی که ساختید
//    private static final String API_KEY = "API_KEY_SHOMA_INJA";
//    private static final int TEMPLATE_ID = 123456; // شناسه قالبی که ساختید
    private static final String URL = "https://api.sms.ir/v1/send/verify";

    private final RestTemplate restTemplate;

    public SmsService() {
        this.restTemplate = new RestTemplate();
    }

    public void sendOtp(String mobile, String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", API_KEY);

        // نام متغیر در قالب شما. مثلا اگر در قالب نوشتید "کد: #CODE#" اینجا باید "CODE" باشد
        SmsParameter param = new SmsParameter("کد", code);

        SmsRequest requestBody = new SmsRequest(mobile, TEMPLATE_ID, Collections.singletonList(param));

        HttpEntity<SmsRequest> entity = new HttpEntity<>(requestBody, headers);

        try {
            System.out.println("Sending SMS with API Key: " + "YOUR_KEY_HERE");
            restTemplate.postForObject(URL, entity, String.class);
            System.out.println("SMS sent to " + mobile + " code: " + code);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("خطا در ارسال پیامک");
        }
    }
}