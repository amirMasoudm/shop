package org.example.shop1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync // برای تریگرِ async اطلاع‌رسانیِ موجودی (تا ذخیرهٔ محصول کند/شکننده نشود)
@EnableScheduling // برای تخلیهٔ دوره‌ایِ صفِ رویدادهای رفتاری (UserEventRecorder.flush)
public class Shop1Application {

    public static void main(String[] args) {
        SpringApplication.run(Shop1Application.class, args);
    }

}
