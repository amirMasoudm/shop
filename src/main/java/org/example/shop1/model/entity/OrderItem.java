package org.example.shop1.model.entity;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

// این کلاس به عنوان یک کلاس داخلی (Embedded) در Order ذخیره می‌شود و Entity جدا نیست
public class OrderItem {

    private String productId;
    private String productName;
    private String productImage; // برای نمایش در تاریخچه سفارشات بدون نیاز به جوین
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal unitPrice; // قیمت واحد در لحظه خرید
    private int quantity; // تعداد سفارش داده شده

    public OrderItem() {}

    public OrderItem(String productId, String productName, String productImage, BigDecimal unitPrice, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.productImage = productImage;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public BigDecimal getTotalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    // Getters & Setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}