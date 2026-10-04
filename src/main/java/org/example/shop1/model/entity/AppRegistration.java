package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * یک ثبت‌نامِ موفق در کلاینت‌های {@code /api/app/v1} — همان چیزی که کاربر در فرم نوشت،
 * جدا از پروفایلِ {@link User}. پروفایل فقط وقتی خالی است از این پر می‌شود؛ این رکورد
 * همیشه عینِ ورودی را نگه می‌دارد تا فهرستِ پنلِ ادمین آن را نشان دهد.
 */
@Document(collection = "app_registrations")
public class AppRegistration {

    @Id
    private String id;
    private String userId;
    private String name;
    private String phone;
    private String company;
    private String installationId;
    private String client;
    private String appVersion;
    private String consentVersion;
    /** {@code sms} یا {@code session} — دومی یعنی کاربرِ ازقبل‌واردشدهٔ سایت. */
    private String method;
    private String tokenId;
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getInstallationId() { return installationId; }
    public void setInstallationId(String installationId) { this.installationId = installationId; }
    public String getClient() { return client; }
    public void setClient(String client) { this.client = client; }
    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
    public String getConsentVersion() { return consentVersion; }
    public void setConsentVersion(String consentVersion) { this.consentVersion = consentVersion; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
