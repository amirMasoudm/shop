package org.example.shop1.model.entity;

import org.example.shop1.model.enums.OrderStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    private String orderCode;

    @DocumentReference
    private User user;

    private List<OrderItem> items = new ArrayList<>();

    // مبلغ کالاها بدون پست
    private BigDecimal itemsTotal = BigDecimal.ZERO;

    // هزینه ارسال
    private BigDecimal shippingCost = BigDecimal.ZERO;

    // مبلغ نهایی
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private OrderStatus status; // تغییر از String به OrderStatus



    /*
     PENDING_PAYMENT
     PAID
     PROCESSING
     SHIPPED
     DELIVERED
     CANCELLED
     */

    private Instant orderDate = Instant.now();

    // ONLINE / POS
    private String type;

    // بعداً در checkout تکمیل می‌شود
    private String shippingAddress;

    // آیا سفارش کامل شده؟
    private boolean finalized = false;

    // آیا پرداخت شده؟
    private boolean paid = false;

    public Order() {}

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public BigDecimal getItemsTotal() {
        return itemsTotal;
    }

    public void setItemsTotal(BigDecimal itemsTotal) {
        this.itemsTotal = itemsTotal;
    }

    public BigDecimal getShippingCost() {
        return shippingCost;
    }

    public void setShippingCost(BigDecimal shippingCost) {
        this.shippingCost = shippingCost;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }



    public Instant getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Instant orderDate) {
        this.orderDate = orderDate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public boolean isFinalized() {
        return finalized;
    }

    public void setFinalized(boolean finalized) {
        this.finalized = finalized;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }
}