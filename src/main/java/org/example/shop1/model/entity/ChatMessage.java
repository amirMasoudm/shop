package org.example.shop1.model.entity;

import org.example.shop1.model.enums.MessageType;
import org.example.shop1.model.enums.SenderRole;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * یک پیام در گفت‌وگوی پشتیبانی.
 * <p>
 * منبعِ حقیقت همین کالکشن است، نه وب‌سوکت. وب‌سوکت فقط شتاب‌دهنده است: اگر اتصال قطع
 * شود کلاینت با {@code GET /api/v1/chat/messages?after=<lastId>} جامانده‌ها را می‌گیرد.
 */
@Document(collection = "messages")
public class ChatMessage {

    /**
     * ضمیمهٔ پیام.
     * <p>
     * 🔴 <b>اینجا عمداً هیچ فیلدِ آدرسِ عمومی‌ای نیست.</b> فقط {@code storedName} (نامِ
     * فایل روی دیسک در {@code /opt/shop/chat/}) ذخیره می‌شود و آدرسِ قابلِ‌استفاده را
     * لایهٔ API از روی شناسهٔ پیام می‌سازد. اگر اینجا فیلدِ {@code url} می‌گذاشتیم، دیر
     * یا زود یک آدرسِ عمومیِ {@code /uploads/...} داخلش می‌نشست — همان اشتباهی که
     * ۳۰ اوت باعث شد فایلِ {@code .env} از مسیرِ آپلود سرو شود.
     * <p>
     * {@code durationMs} از حالا هست ولی فعلاً همیشه {@code null} است؛ جای ویسِ فازِ ۱-ب.
     */
    public static class Attachment {
        private String storedName;
        private String name;
        private long sizeBytes;
        private String mime;
        private Long durationMs;

        public String getStoredName() { return storedName; }
        public void setStoredName(String storedName) { this.storedName = storedName; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public long getSizeBytes() { return sizeBytes; }
        public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

        public String getMime() { return mime; }
        public void setMime(String mime) { this.mime = mime; }

        public Long getDurationMs() { return durationMs; }
        public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    }

    @Id
    private String id;

    private String conversationId;

    /** برای پیامِ {@code SYSTEM} نال است. */
    private String senderId;

    private SenderRole senderRole;

    /** نامِ نمایشیِ فرستنده در لحظهٔ ارسال — تا تغییرِ بعدیِ نامِ کاربر تاریخچه را عوض نکند. */
    private String senderName;

    private MessageType type = MessageType.TEXT;

    /** متنِ پیام، یا کپشنِ ضمیمه. همیشه متنِ ساده است (تگ‌ها در سرویس حذف می‌شوند). */
    private String body;

    private Attachment attachment;

    /** فقط شناسه؛ متنِ نقل‌شده را کلاینت از روی همان پیام رندر می‌کند. */
    private String replyToId;

    private Instant createdAt = Instant.now();
    private Instant deliveredAt;
    private Instant readAt;
    private Instant editedAt;
    private Instant deletedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public SenderRole getSenderRole() { return senderRole; }
    public void setSenderRole(SenderRole senderRole) { this.senderRole = senderRole; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Attachment getAttachment() { return attachment; }
    public void setAttachment(Attachment attachment) { this.attachment = attachment; }

    public String getReplyToId() { return replyToId; }
    public void setReplyToId(String replyToId) { this.replyToId = replyToId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }

    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }

    public Instant getEditedAt() { return editedAt; }
    public void setEditedAt(Instant editedAt) { this.editedAt = editedAt; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
