package org.example.shop1.model.service;

import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.OtpData;
import org.example.shop1.model.entity.User;
import org.example.shop1.model.enums.Role;
import org.example.shop1.model.reposritory.OtpRepository;
import org.example.shop1.model.reposritory.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final SmsService smsService;
    private final OtpRepository otpRepository;

    // تولید کد امن (به جای java.util.Random که قابل پیش‌بینی است)
    private final SecureRandom secureRandom = new SecureRandom();

    private static final long OTP_EXPIRE_TIME = 120000; //2 دقیقه

    private static final long OTP_REQUEST_INTERVAL = 60000; //60 ثانیه

    private static final int MAX_ATTEMPTS = 5;

    public AuthService(UserRepository userRepository, SmsService smsService, OtpRepository otpRepository) {
        this.userRepository = userRepository;
        this.smsService = smsService;
        this.otpRepository = otpRepository;
    }


    /*
        ارسال OTP
    */
    public void sendOtpCode(String phoneNumber) {

        validatePhone(phoneNumber);

        // محدودیت نرخ: اگر کد فعالی وجود دارد که کمتر از فاصله مجاز از آن گذشته، اجازه نده
        OtpData existing = otpRepository.findById(phoneNumber).orElse(null);
        if (existing != null
                && System.currentTimeMillis() - existing.getCreatedAt() < OTP_REQUEST_INTERVAL) {
            long waitSeconds = (OTP_REQUEST_INTERVAL - (System.currentTimeMillis() - existing.getCreatedAt())) / 1000 + 1;
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "کد قبلاً ارسال شده؛ لطفاً " + waitSeconds + " ثانیه دیگر دوباره تلاش کنید");
        }

        // کد ۵ رقمی امن (بازه 10000..99999 مطابق رفتار قبلی حفظ شده)
        String code = String.valueOf(10000 + secureRandom.nextInt(90000));

        OtpData otp = new OtpData(phoneNumber, code, System.currentTimeMillis() + OTP_EXPIRE_TIME);

        // فقط در محیط توسعه (سطح DEBUG) دیده می‌شود؛ در prod لاگ نمی‌شود
        log.debug("OTP for {} = {}", phoneNumber, code);

        otpRepository.save(otp);

        smsService.sendOtp(phoneNumber, code);
    }


    /*
       فقط بررسی OTP
       مخصوص Admin
    */
    public boolean verifyOnlyCode(String phoneNumber, String code) {

        OtpData otp = otpRepository.findById(phoneNumber).orElse(null);

        if (otp == null) {
            return false;
        }

        if (otp.isExpired()) {
            otpRepository.deleteById(phoneNumber);
            return false;
        }

        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            otpRepository.deleteById(phoneNumber);
            return false;
        }

        if (!otp.getCode().equals(code)) {
            otp.increaseAttempts();
            otpRepository.save(otp);
            return false;
        }

        /*
           OTP مصرف شد
        */
        otpRepository.deleteById(phoneNumber);

        return true;
    }


    /*
       Login مشتری با OTP
    */
    /**
     * نتیجهٔ ورود با کدِ پیامکی.
     * <p>
     * {@code created} لازم است چون فقط همین‌جا معلوم است که کاربر تازه ساخته شد یا از
     * قبل بود. کنترلر قبلاً این را از «خالی‌بودنِ نام» حدس می‌زد، که غلط بود: مشتریِ
     * برگشتی که پروفایلش را پر نکرده، هر بار {@code REGISTER} ثبت می‌شد و شمارشِ
     * ثبت‌نام برای همیشه باد می‌کرد.
     */
    public record LoginResult(User user, boolean created) {}

    public LoginResult verifyCodeAndLogin(String phoneNumber, String code) {

        if (!verifyOnlyCode(phoneNumber, code)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "کد تایید اشتباه است یا منقضی شده");
        }

        return userRepository.findByPhoneNumber(phoneNumber)
                .map(existing -> new LoginResult(existing, false))
                .orElseGet(() -> new LoginResult(
                        userRepository.save(new User(phoneNumber, Role.USER)), true));
    }


    private void validatePhone(String phone) {
        if (phone == null || !phone.matches("^09\\d{9}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "شماره موبایل معتبر نیست");
        }
    }
}
