# ============================================================
# ایمیج اجرا — از jar از پیش‌ساخته‌شده استفاده می‌کند
# قبل از build حتماً یک بار لوکال بیلد کن:
#     mvn clean package -DskipTests
# (بیلد داخل داکر با mvn به‌خاطر گیرکردن روی مخزن mvnhub در تحریم کند است،
#  به همین دلیل عمداً jar لوکال کپی می‌شود)
# ============================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# منطقه زمانی برای درست بودن تاریخ سفارش‌ها
ENV TZ=Asia/Tehran

# پوشه آپلودها داخل کانتینر
RUN mkdir -p /opt/shop/uploads && chmod 777 /opt/shop/uploads

# کپی jar ساخته‌شده (خروجی mvn package)
COPY target/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
