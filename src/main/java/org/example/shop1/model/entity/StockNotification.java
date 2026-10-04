package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

// درخواستِ «اطلاع بده وقتی موجود شد» برای یک محصولِ ناموجود
@Document(collection = "stock_notifications")
@CompoundIndex(name = "productId_notified_idx", def = "{'productId': 1, 'notified': 1}")
public class StockNotification {

    @Id
    private String id;

    private String productId;
    private String mobile;      // از سشنِ احرازهویت‌شده گرفته می‌شود، نه ورودیِ کاربر
    private Instant createdAt = Instant.now();
    private boolean notified = false;

    public StockNotification() {}

    public StockNotification(String productId, String mobile) {
        this.productId = productId;
        this.mobile = mobile;
        this.createdAt = Instant.now();
        this.notified = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public boolean isNotified() { return notified; }
    public void setNotified(boolean notified) { this.notified = notified; }
}
