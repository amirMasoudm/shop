package org.example.shop1.model.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// ورودی‌های APIهای RFQ
public class RfqRequestDto {

    // ثبت استعلام از سبد
    public static class CreateRequest {
        private List<Line> items = new ArrayList<>();
        public List<Line> getItems() { return items; }
        public void setItems(List<Line> items) { this.items = items; }
    }

    public static class Line {
        private String productId;
        private int quantity;
        private BigDecimal proposedUnitPrice; // اختیاری
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public BigDecimal getProposedUnitPrice() { return proposedUnitPrice; }
        public void setProposedUnitPrice(BigDecimal proposedUnitPrice) { this.proposedUnitPrice = proposedUnitPrice; }
    }

    // پیشنهاد قیمت (چانه‌زنی) از کاربر یا ادمین
    public static class OfferRequest {
        private BigDecimal amount;
        private String note;
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }
}
