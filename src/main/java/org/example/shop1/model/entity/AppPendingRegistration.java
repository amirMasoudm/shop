package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * نام و شرکتِ واردشده تا تأییدِ کدِ پیامکی — حداکثر ۱۰ دقیقه (ایندکسِ TTL روی
 * {@code createdAt} در {@code AppApiIndexes}). کلید ترکیبِ شماره و installationId است،
 * پس دستگاهِ دیگری نمی‌تواند نامِ واردشده در این دستگاه را تأیید کند.
 */
@Document(collection = "app_pending_registrations")
public class AppPendingRegistration {

    @Id
    private String id;
    private String phone;
    private String installationId;
    private String name;
    private String company;
    private String client;
    private String appVersion;
    private String consentVersion;
    private Instant createdAt;

    public static String key(String phone, String installationId) {
        return phone + ":" + installationId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getInstallationId() { return installationId; }
    public void setInstallationId(String installationId) { this.installationId = installationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getClient() { return client; }
    public void setClient(String client) { this.client = client; }
    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
    public String getConsentVersion() { return consentVersion; }
    public void setConsentVersion(String consentVersion) { this.consentVersion = consentVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
