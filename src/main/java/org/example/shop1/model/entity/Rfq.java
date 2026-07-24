package org.example.shop1.model.entity;

import org.example.shop1.model.enums.RfqStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// استعلام پیش‌فاکتور (Request For Quotation)
@Document(collection = "rfqs")
public class Rfq {

    @Id
    private String id;

    private String code; // مثل RFQ-482913

    @DocumentReference
    private User user;

    private List<RfqItem> items = new ArrayList<>();

    // جمع مرجع سروری اقلام در لحظه‌ی ثبت (بر اساس قیمت فروشگاه) — برای اعتبارسنجی آستانه
    private BigDecimal itemsListTotal = BigDecimal.ZERO;

    private RfqStatus status = RfqStatus.PENDING;

    // مبلغ کل نهاییِ پیشنهادیِ ادمین (آخرین quote)
    private BigDecimal adminTotalAmount;

    // تاریخچه‌ی چانه‌زنی
    private List<RfqOffer> offers = new ArrayList<>();

    // در صورت accept، شناسه‌ی سفارش ساخته‌شده
    private String linkedOrderId;

    private Instant createdAt = Instant.now();
    private Instant expiresAt;

    public Rfq() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public List<RfqItem> getItems() { return items; }
    public void setItems(List<RfqItem> items) { this.items = items; }
    public BigDecimal getItemsListTotal() { return itemsListTotal; }
    public void setItemsListTotal(BigDecimal itemsListTotal) { this.itemsListTotal = itemsListTotal; }
    public RfqStatus getStatus() { return status; }
    public void setStatus(RfqStatus status) { this.status = status; }
    public BigDecimal getAdminTotalAmount() { return adminTotalAmount; }
    public void setAdminTotalAmount(BigDecimal adminTotalAmount) { this.adminTotalAmount = adminTotalAmount; }
    public List<RfqOffer> getOffers() { return offers; }
    public void setOffers(List<RfqOffer> offers) { this.offers = offers; }
    public String getLinkedOrderId() { return linkedOrderId; }
    public void setLinkedOrderId(String linkedOrderId) { this.linkedOrderId = linkedOrderId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
