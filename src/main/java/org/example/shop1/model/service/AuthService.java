package org.example.shop1.model.service;

import org.example.shop1.model.entity.User;
import org.example.shop1.model.enums.Role;
import org.example.shop1.model.reposritory.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SmsService smsService;

    // ذخیره موقت: Key=شماره موبایل, Value=کد
    // در محیط واقعی بهتر است از Redis با TTL استفاده شود
    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository, SmsService smsService) {
        this.userRepository = userRepository;
        this.smsService = smsService;
    }

    // مرحله ۱: تولید کد و ارسال
    public void sendOtpCode(String phoneNumber) {
        String code = String.valueOf(new Random().nextInt(90000) + 10000); // کد ۵ رقمی
        otpStorage.put(phoneNumber, code);
        smsService.sendOtp(phoneNumber, code);
    }

// در کلاس AuthService

    public boolean verifyOnlyCode(String phoneNumber, String code) {
        String savedCode = otpStorage.get(phoneNumber);
        if (savedCode != null && savedCode.equals(code)) {
            otpStorage.remove(phoneNumber);
            return true;
        }
        return false;
    }

    // متد قبلی شما باید اصلاح شود تا نقش پیش فرض USER بدهد و باگ admin حذف شود
    public User verifyCodeAndLogin(String phoneNumber, String code) {
        if (verifyOnlyCode(phoneNumber, code)) {
            return userRepository.findByPhoneNumber(phoneNumber)
                    .orElseGet(() -> {
                        // کاربران عادی که با شماره ثبت نام میکنند
                        User newUser = new User(phoneNumber, Role.USER);
                        return userRepository.save(newUser);
                    });
        } else {
            throw new RuntimeException("کد وارد شده صحیح نیست!");
        }
    }

}