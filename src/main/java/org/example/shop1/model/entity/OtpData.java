package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "otp_codes")
public class OtpData {

    @Id
    private String phoneNumber;

    private String code;

    // اعتبار کد (epoch millis)
    private long expireAt;

    // زمان ساخت (epoch millis) — برای محدودیت نرخ درخواست
    private long createdAt;

    private int attempts;

    // ایندکس TTL: مونگو خودش رکورد را حدود ۵ دقیقه بعد پاک می‌کند تا انباشت نشود
    @Indexed(expireAfterSeconds = 300)
    private Instant ttlAt = Instant.now();

    public OtpData() {}

    public OtpData(String phoneNumber, String code, long expireAt) {
        this.phoneNumber = phoneNumber;
        this.code = code;
        this.expireAt = expireAt;
        this.createdAt = System.currentTimeMillis();
        this.attempts = 0;
        this.ttlAt = Instant.now();
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCode() {
        return code;
    }

    public long getExpireAt() {
        return expireAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public void increaseAttempts() {
        this.attempts++;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expireAt;
    }
}
