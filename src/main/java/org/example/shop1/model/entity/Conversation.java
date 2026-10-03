package org.example.shop1.model.entity;

import org.example.shop1.model.enums.ConversationStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * گفت‌وگوی پشتیبانی — <b>یکی به‌ازای هر مشتری، نه یکی به‌ازای هر تیکت</b>.
 * <p>
 * دلیلِ این انتخاب: مشتریِ تجهیزاتِ شبکه معمولاً چند بار برمی‌گردد و سؤالِ بعدی‌اش
 * ادامهٔ قبلی است. اگر هر بار تیکتِ تازه ساخته می‌شد، کارشناس تاریخچه را نمی‌دید و
 * مشتری مجبور می‌شد خودش را تکرار کند. پس {@code customerId} یکتاست و گفت‌وگوی
 * بسته‌شده با پیامِ بعدی دوباره باز می‌شود، نه اینکه دومی ساخته شود.
 * <p>
 * فیلدهای {@code customerName} و {@code assignedAgentName} عمداً denormalize شده‌اند:
 * صفِ کارشناس باید ده‌ها گفت‌وگو را یک‌جا نشان دهد و بدونِ این‌ها هر ردیف یک کوئریِ
 * اضافه به {@code users} می‌خواست.
 */
@Document(collection = "conversations")
public class Conversation {

    @Id
    private String id;

    /** شناسهٔ کاربرِ مشتری. یکتاست — نگاه کن به توضیحِ بالای کلاس. */
    private String customerId;

    private String customerName;

    /**
     * کارشناسِ تصاحب‌کننده؛ تا وقتی {@code null} است گفت‌وگو در صفِ مشترک می‌ماند.
     * <p>
     * ⚠️ تصاحب باید اتمیک باشد (findAndModify با پیش‌شرطِ {@code null} بودنِ همین فیلد).
     * خواندن-سپس-نوشتن باعث می‌شود دو کارشناسی که هم‌زمان کلیک کرده‌اند هر دو مالک شوند.
     */
    private String assignedAgentId;

    private String assignedAgentName;

    private ConversationStatus status = ConversationStatus.OPEN;

    private Instant lastMessageAt;

    /** پیش‌نمایشِ کوتاهِ آخرین پیام برای فهرستِ صف — تا برایِ هر ردیف پیام‌ها خوانده نشود. */
    private String lastMessagePreview;

    private int unreadForCustomer;
    private int unreadForAgent;

    private Instant createdAt = Instant.now();
    private Instant closedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getAssignedAgentId() { return assignedAgentId; }
    public void setAssignedAgentId(String assignedAgentId) { this.assignedAgentId = assignedAgentId; }

    public String getAssignedAgentName() { return assignedAgentName; }
    public void setAssignedAgentName(String assignedAgentName) { this.assignedAgentName = assignedAgentName; }

    public ConversationStatus getStatus() { return status; }
    public void setStatus(ConversationStatus status) { this.status = status; }

    public Instant getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Instant lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public String getLastMessagePreview() { return lastMessagePreview; }
    public void setLastMessagePreview(String lastMessagePreview) { this.lastMessagePreview = lastMessagePreview; }

    public int getUnreadForCustomer() { return unreadForCustomer; }
    public void setUnreadForCustomer(int unreadForCustomer) { this.unreadForCustomer = unreadForCustomer; }

    public int getUnreadForAgent() { return unreadForAgent; }
    public void setUnreadForAgent(int unreadForAgent) { this.unreadForAgent = unreadForAgent; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
}
