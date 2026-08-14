package org.example.shop1.config;

import org.example.shop1.model.entity.User;
import org.example.shop1.model.enums.Role;
import org.example.shop1.model.reposritory.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@Order(Ordered.LOWEST_PRECEDENCE) // بعد از MongoIndexInitializer اجرا شود
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

        // existsByUsername به‌جای findByUsername: اگر رکوردِ تکراری در دیتابیس باشد،
        // findBy... با IncorrectResultSizeDataAccessException می‌ترکد و جلوی بالا آمدنِ
        // برنامه را می‌گیرد — همان اتفاقی که یک‌بار سرِ ری‌استورِ دیتا افتاد.
        if (userRepository.existsByUsername(adminUsername)) {
            return;
        }

        // phoneNumber هم ایندکسِ unique دارد؛ اگر کاربرِ دیگری با همین شماره ثبت‌نام کرده باشد
        // ساختِ ادمین با DuplicateKeyException رد می‌شود. زودتر و با پیامِ روشن ردش می‌کنیم.
        if (userRepository.existsByPhoneNumber(adminPhone)) {
            log.warn("⚠️ سوپرادمین ساخته نشد: شماره {} قبلاً به کاربرِ دیگری تعلق دارد. "
                    + "یا آن کاربر را حذف کن یا نقشش را به ADMIN تغییر بده.", adminPhone);
            return;
        }

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setFirstName("sample");
        admin.setPhoneNumber(adminPhone);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setAddresses(new ArrayList<>());

        try {
            userRepository.save(admin);
            log.info("✅ سوپر ادمین سیستم با موفقیت ایجاد شد.");
        } catch (DuplicateKeyException e) {
            // فاصلهٔ بینِ چکِ بالا و save؛ نمونهٔ دیگری از برنامه زودتر ساختش. بی‌خطر است.
            log.info("سوپرادمین هم‌زمان توسط نمونهٔ دیگری ساخته شد؛ از ساختِ دوباره صرف‌نظر شد.");
        }
    }
}