package org.example.shop1.model.entity;

import org.example.shop1.model.enums.OrderStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
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
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal itemsTotal = BigDecimal.ZERO;

    // ۱۰٪ مالیات بر ارزش افزوده — همیشه سمتِ سرور محاسبه می‌شود (OrderService.applyTotals)
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    // مبلغ نهایی = itemsTotal + taxAmount (هزینهٔ ارسال دیگر تویِ جمع نیست — پس‌کرایه است)
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // فقط نامِ روشِ انتخابیِ مشتری (تیپاکس/باربری/پست...)؛ چون دیگر قیمتی نداریم،
    // این تنها اثرِ ماندگارِ انتخابِ ارسال است.
    private String shippingMethod;

    // ===== درگاهِ پرداختِ ملت =====
    // orderIdِ عددیِ ارسالی به بانک — جدا از orderCode (که برایِ این کار امن نیست، رجوع به پرامپت)
    private Long paymentRefNumber;
    // RefId که بانک بعدِ bpPayRequestِ موفق برمی‌گرداند؛ برایِ ساختِ صفحهٔ auto-submit لازم است
    private String mellatRefId;
    // SaleReferenceIdِ نهایی — بعدِ callbackِ موفق ذخیره می‌شود، برایِ verify/settle/reversal لازم است
    private String mellatSaleReferenceId;
    // اگر پرداخت شکست خورد، دلیل اینجا ثبت می‌شود تا پشتیبانی بدونِ کاوش در لاگ ببیندش
    private String paymentFailureReason;

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

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getShippingMethod() {
        return shippingMethod;
    }

    public void setShippingMethod(String shippingMethod) {
        this.shippingMethod = shippingMethod;
    }

    public Long getPaymentRefNumber() {
        return paymentRefNumber;
    }

    public void setPaymentRefNumber(Long paymentRefNumber) {
        this.paymentRefNumber = paymentRefNumber;
    }

    public String getMellatRefId() {
        return mellatRefId;
    }

    public void setMellatRefId(String mellatRefId) {
        this.mellatRefId = mellatRefId;
    }

    public String getMellatSaleReferenceId() {
        return mellatSaleReferenceId;
    }

    public void setMellatSaleReferenceId(String mellatSaleReferenceId) {
        this.mellatSaleReferenceId = mellatSaleReferenceId;
    }

    public String getPaymentFailureReason() {
        return paymentFailureReason;
    }

    public void setPaymentFailureReason(String paymentFailureReason) {
        this.paymentFailureReason = paymentFailureReason;
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