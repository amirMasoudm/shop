package org.example.shop1.model.entity;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

// یک ردیف از تاریخچه‌ی چانه‌زنی (Embedded در Rfq)
public class RfqOffer {

    private String by;        // "USER" یا "ADMIN"
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount; // مبلغ کل پیشنهادی این گام
    private String note;       // توضیح اختیاری
    private Instant at = Instant.now();

    public RfqOffer() {}

    public RfqOffer(String by, BigDecimal amount, String note) {
        this.by = by;
        this.amount = amount;
        this.note = note;
        this.at = Instant.now();
    }

    public String getBy() { return by; }
    public void setBy(String by) { this.by = by; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Instant getAt() { return at; }
    public void setAt(Instant at) { this.at = at; }
}
