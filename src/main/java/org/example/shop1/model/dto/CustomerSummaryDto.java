package org.example.shop1.model.dto;

import org.example.shop1.model.entity.User;

import java.time.Instant;

/**
 * نمایِ مشتری برایِ پنلِ فروشِ حضوری — <b>بدونِ شمارهٔ تلفن</b>.
 * <p>
 * 🔴 <b>چرا DTO و نه پنهان‌کردن در UI:</b> اگر خودِ {@code User} فرستاده می‌شد،
 * شماره در پاسخِ JSON می‌ماند و هر کسی با ابزارِ توسعهٔ مرورگر می‌دیدش. خواستهٔ
 * مالک «نمایش داده نشود» است، و تنها پیاده‌سازیِ درستش «فرستاده نشود» است.
 * <p>
 * ⚠️ در این اپ {@code username} خودش شمارهٔ موبایل است، پس آن هم بیرون است — نه
 * فقط فیلدِ {@code phoneNumber}. به‌جایش یک شکلِ پوشانده می‌آید که کارشناس بتواند
 * تماس‌گیرنده را تطبیق بدهد بی‌آنکه عدد کامل جایی برود.
 * <p>
 * نقش هم می‌آید تا پنل بتواند نشان بدهد این فهرست فقط مشتری است، نه کارمند.
 */
public class CustomerSummaryDto {

    private final String id;
    private final String firstName;
    private final String lastName;
    private final String maskedPhone;
    private final Instant createdAt;
    private final String role;
    /** شمارهٔ سفارش‌ها — توی سازنده شمرده نمی‌شود چون یک کوئری به‌ازای هر ردیف می‌شد. */
    private Integer orderCount;

    public CustomerSummaryDto(User u) {
        this.id = u.getId();
        this.firstName = u.getFirstName();
        this.lastName = u.getLastName();
        this.maskedPhone = mask(u.getPhoneNumber() != null && !u.getPhoneNumber().isBlank()
                ? u.getPhoneNumber() : u.getUsername());
        this.createdAt = u.getCreatedAt();
        this.role = u.getRole() == null ? null : u.getRole().name();
    }

    public static CustomerSummaryDto of(User u) {
        return new CustomerSummaryDto(u);
    }

    /**
     * {@code 09130770075} → {@code 0913****075}.
     * <p>
     * چهار رقمِ میانی پوشانده می‌شود، نه ابتدا و انتها: پیش‌شماره و چند رقمِ آخر
     * برایِ تشخیصِ «همین نفر بود؟» کافی‌اند، ولی عددِ کامل بازسازی‌شدنی نیست.
     */
    public static String mask(String raw) {
        if (raw == null) return null;
        String d = raw.trim();
        if (d.length() < 8) return "—";
        return d.substring(0, 4) + "****" + d.substring(d.length() - 3);
    }

    public String getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getMaskedPhone() { return maskedPhone; }
    public Instant getCreatedAt() { return createdAt; }
    public String getRole() { return role; }
    public Integer getOrderCount() { return orderCount; }
    public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }
}
