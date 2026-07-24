package org.example.shop1.model.entity;

import java.math.BigDecimal;

// اسنپ‌شات یک قلم از سبد در لحظه‌ی ثبت استعلام (Embedded در Rfq، مثل OrderItem)
public class RfqItem {

    private String productId;
    private String productName;
    private String productImage;
    private int quantity;
    private BigDecimal listUnitPrice;      // قیمت واحد فروشگاه در لحظه‌ی ثبت (مرجع سروری)
    private BigDecimal proposedUnitPrice;  // قیمت پیشنهادی کاربر (اختیاری)

    public RfqItem() {}

    public RfqItem(String productId, String productName, String productImage, int quantity,
                   BigDecimal listUnitPrice, BigDecimal proposedUnitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.productImage = productImage;
        this.quantity = quantity;
        this.listUnitPrice = listUnitPrice;
        this.proposedUnitPrice = proposedUnitPrice;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getListUnitPrice() { return listUnitPrice; }
    public void setListUnitPrice(BigDecimal listUnitPrice) { this.listUnitPrice = listUnitPrice; }
    public BigDecimal getProposedUnitPrice() { return proposedUnitPrice; }
    public void setProposedUnitPrice(BigDecimal proposedUnitPrice) { this.proposedUnitPrice = proposedUnitPrice; }
}
