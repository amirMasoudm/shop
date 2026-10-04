package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * توکنِ دستگاهِ کلاینت‌های {@code /api/app/v1}: اپِ اندرویدی یا ابزارِ سایت.
 * <p>
 * 🔴 خودِ توکن هرگز ذخیره نمی‌شود؛ فقط هشِ SHA-256ـش در {@code tokenHash}. کسی که به
 * دیتابیس دست پیدا کند با این رکوردها نمی‌تواند خودش را جای کاربر بزند.
 * <p>
 * انقضای زمانی ندارد؛ فقط ابطال ({@code revokedAt}). {@code active} جدا از
 * {@code revokedAt} نگه داشته می‌شود چون ایندکسِ «یک توکنِ فعال برای هر
 * installationId» با partialFilter روی {@code active: true} ساخته می‌شود.
 * ایندکس‌ها در {@code AppApiIndexes} ساخته می‌شوند، نه با انوتیشن.
 */
@Document(collection = "app_tokens")
public class AppToken {

    @Id
    private String id;
    private String tokenHash;
    private String userId;
    private String installationId;
    private String client;
    private Instant createdAt;
    private Instant lastUsedAt;
    private Instant revokedAt;
    private boolean active;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getInstallationId() { return installationId; }
    public void setInstallationId(String installationId) { this.installationId = installationId; }
    public String getClient() { return client; }
    public void setClient(String client) { this.client = client; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
