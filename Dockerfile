# استفاده از ایمیج سبک جاوا (اگر جاوای شما 21 است، عدد 17 را به 21 تغییر دهید)
FROM eclipse-temurin:17-jre-alpine

# تنظیم منطقه زمانی (اختیاری اما به شدت توصیه شده برای سفارشات)
ENV TZ=Asia/Tehran

# ساخت پوشه آپلودها در داخل کانتینر
RUN mkdir -p /opt/shop/uploads && chmod 777 /opt/shop/uploads

# کپی کردن فایل بیلد شده اسپرینگ‌بوت به داخل کانتینر داکر
COPY target/*.jar app.jar

# دستوری که هنگام روشن شدن کانتینر اجرا می‌شود
ENTRYPOINT ["java", "-jar", "/app.jar"]