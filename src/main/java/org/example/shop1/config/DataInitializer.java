package org.example.shop1.config;

import org.example.shop1.model.entity.User;
import org.example.shop1.model.enums.Role;
import org.example.shop1.model.reposritory.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // مقادیر اولیه ادمین از متغیر محیطی خوانده می‌شوند؛ fallback فقط برای راحتی محیط توسعه است
    @Value("${app.admin.username:superadmin}")
    private String adminUsername;

    @Value("${app.admin.phone:09130770075}")
    private String adminPhone;

    @Value("${app.admin.password:Admin@12345!}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // اگر رمز یا شماره تنظیم نشده باشد (مثلاً در prod بدون env)، ادمینِ ناامن نساز
        if (adminPassword == null || adminPassword.isBlank() || adminPhone == null || adminPhone.isBlank()) {
            log.warn("ادمین اولیه ساخته نشد: ADMIN_PASSWORD/ADMIN_PHONE تنظیم نشده است.");
            return;
        }

        // ایجاد ادمین اولیه سیستم به صورت خودکار در صورت عدم وجود
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setFirstName("sample");
            admin.setPhoneNumber(adminPhone);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            admin.setAddresses(new ArrayList<>());

            userRepository.save(admin);
            log.info("✅ سوپر ادمین سیستم با موفقیت ایجاد شد.");
        }
    }
}