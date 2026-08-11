package org.example.shop1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // برای تریگرِ async اطلاع‌رسانیِ موجودی (تا ذخیرهٔ محصول کند/شکننده نشود)
public class Shop1Application {

    public static void main(String[] args) {
        SpringApplication.run(Shop1Application.class, args);
    }

}
