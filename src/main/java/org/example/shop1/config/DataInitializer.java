package org.example.shop1.config;

import org.example.shop1.model.entity.User;
import org.example.shop1.model.enums.Role;
import org.example.shop1.model.reposritory.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // ایجاد ادمین اولیه سیستم به صورت خودکار در صورت عدم وجود
        if (userRepository.findByUsername("superadmin").isEmpty()) {
            User admin = new User();
            admin.setUsername("superadmin");
            admin.setFirstName("sample");
            // شماره موبایل واقعی ادمین را اینجا قرار دهید تا پیامک OTP به آن ارسال شود
            admin.setPhoneNumber("09130770075");
            admin.setPassword(passwordEncoder.encode("Admin@12345!")); // حتماً در محیط واقعی یک رمز قوی قرار دهید
            admin.setRole(Role.ADMIN);
            admin.setAddresses(new ArrayList<>());

            userRepository.save(admin);
            System.out.println("✅ سوپر ادمین سیستم با موفقیت ایجاد شد.");
        }
    }
}