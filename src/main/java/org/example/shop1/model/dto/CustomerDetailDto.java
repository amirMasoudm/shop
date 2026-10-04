package org.example.shop1.model.dto;

import org.example.shop1.model.entity.Address;
import org.example.shop1.model.entity.Comment;
import org.example.shop1.model.entity.Order;
import org.example.shop1.model.entity.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * پروندهٔ یک مشتری برایِ پنلِ فروشِ حضوری — <b>هیچ شمارهٔ کاملی در آن نیست</b>.
 * <p>
 * 🔴 <b>چرا DTO و نه فیلترکردن در جاوااسکریپت:</b> خواستهٔ مالک «شماره دیده نشود»
 * است و تنها پیاده‌سازیِ درستش «شماره فرستاده نشود» است. در پنلِ ادمین همین صفحه
 * شمارهٔ کامل را در پاسخِ JSON دارد و فقط ستونش فرق می‌کند؛ اینجا عمداً این‌طور
 * نیست، چون کارشناسِ فروش با ابزارِ توسعهٔ مرورگر می‌توانست همان را بخواند.
 * <p>
 * ⚠️ سه جا شماره دارد و هر سه پوشانده می‌شوند، نه فقط اولی:
 * {@code username} (که در این اپ خودش موبایل است)، {@code phoneNumber}، و
 * {@code recipientPhone}ِ هر نشانی.
 */
public class CustomerDetailDto {

    private final String id;
    private final String firstName;
    private final String lastName;
    private final String maskedPhone;
    private final Instant createdAt;
    private final List<AddressView> addresses = new ArrayList<>();
    private final List<OrderView> orders = new ArrayList<>();
    private final List<CommentView> comments = new ArrayList<>();
    private final BigDecimal totalSpent;

    public CustomerDetailDto(User u, List<Order> userOrders, List<Comment> userComments) {
        this.id = u.getId();
        this.firstName = u.getFirstName();
        this.lastName = u.getLastName();
        this.maskedPhone = CustomerSummaryDto.mask(
                u.getPhoneNumber() != null && !u.getPhoneNumber().isBlank()
                        ? u.getPhoneNumber() : u.getUsername());
        this.createdAt = u.getCreatedAt();

        if (u.getAddresses() != null) {
            u.getAddresses().forEach(a -> addresses.add(new AddressView(a)));
        }
        BigDecimal spent = BigDecimal.ZERO;
        if (userOrders != null) {
            for (Order o : userOrders) {
                orders.add(new OrderView(o));
                if (o.getTotalAmount() != null) spent = spent.add(o.getTotalAmount());
            }
        }
        this.totalSpent = spent;
        if (userComments != null) {
            userComments.forEach(c -> comments.add(new CommentView(c)));
        }
    }

    public String getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getMaskedPhone() { return maskedPhone; }
    public Instant getCreatedAt() { return createdAt; }
    public List<AddressView> getAddresses() { return addresses; }
    public List<OrderView> getOrders() { return orders; }
    public List<CommentView> getComments() { return comments; }
    public BigDecimal getTotalSpent() { return totalSpent; }

    /** نشانی بدونِ شمارهٔ گیرنده — فقط شکلِ پوشانده. */
    public static class AddressView {
        private final String recipientName;
        private final String maskedRecipientPhone;
        private final String fullAddress;
        private final String postalCode;
        private final String state;
        private final String city;

        AddressView(Address a) {
            this.recipientName = a.getRecipientName();
            this.maskedRecipientPhone = CustomerSummaryDto.mask(a.getRecipientPhone());
            this.fullAddress = a.getFullAddress();
            this.postalCode = a.getPostalCode();
            this.state = a.getState();
            this.city = a.getCity();
        }

        public String getRecipientName() { return recipientName; }
        public String getMaskedRecipientPhone() { return maskedRecipientPhone; }
        public String getFullAddress() { return fullAddress; }
        public String getPostalCode() { return postalCode; }
        public String getState() { return state; }
        public String getCity() { return city; }
    }

    /**
     * خلاصهٔ فاکتور. ⚠️ خودِ {@code Order} فرستاده نمی‌شود چون فیلدِ {@code user}
     * را درونِ خودش دارد و شمارهٔ کامل از همان‌جا بیرون می‌زد.
     */
    public static class OrderView {
        private final String id;
        private final String orderCode;
        private final Instant orderDate;
        private final BigDecimal totalAmount;
        private final String status;

        OrderView(Order o) {
            this.id = o.getId();
            this.orderCode = o.getOrderCode();
            this.orderDate = o.getOrderDate();
            this.totalAmount = o.getTotalAmount();
            this.status = o.getStatus() == null ? null : o.getStatus().name();
        }

        public String getId() { return id; }
        public String getOrderCode() { return orderCode; }
        public Instant getOrderDate() { return orderDate; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public String getStatus() { return status; }
    }

    /** نظرِ همین مشتری. {@code userName} عمداً نمی‌آید — نامش بالای پرونده هست. */
    public static class CommentView {
        private final String id;
        private final String productId;
        private final String text;
        private final Integer rating;
        private final String status;
        private final Instant createdAt;

        CommentView(Comment c) {
            this.id = c.getId();
            this.productId = c.getProductId();
            this.text = c.getText();
            this.rating = c.getRating();
            this.status = c.getStatus();
            this.createdAt = c.getCreatedAt();
        }

        public String getId() { return id; }
        public String getProductId() { return productId; }
        public String getText() { return text; }
        public Integer getRating() { return rating; }
        public String getStatus() { return status; }
        public Instant getCreatedAt() { return createdAt; }
    }
}
