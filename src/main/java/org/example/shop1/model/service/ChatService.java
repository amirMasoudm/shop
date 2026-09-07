package org.example.shop1.model.service;

import org.example.shop1.config.SecurityUtils;
import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.ChatMessage;
import org.example.shop1.model.entity.Conversation;
import org.example.shop1.model.entity.User;
import org.example.shop1.model.enums.ConversationStatus;
import org.example.shop1.model.enums.MessageType;
import org.example.shop1.model.enums.Role;
import org.example.shop1.model.enums.SenderRole;
import org.example.shop1.model.reposritory.ChatMessageRepository;
import org.example.shop1.model.reposritory.ConversationRepository;
import org.jsoup.parser.Parser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * منطقِ چتِ پشتیبانی: گفت‌وگو، پیام، صف و تصاحب.
 * <p>
 * قاعدهٔ کلی: <b>دیتابیس منبعِ حقیقت است و وب‌سوکت فقط شتاب‌دهنده.</b> هر پیام اول
 * ذخیره می‌شود و بعد پخش؛ اگر پخش شکست بخورد کاربر با
 * {@code GET /api/v1/chat/messages?after=...} همان را می‌گیرد.
 */
@Service
public class ChatService {

    /** سقفِ نرخِ ارسالِ پیام برای هر کاربر. */
    private static final int MAX_MESSAGES = 20;
    private static final long MESSAGE_WINDOW_MS = 60_000;

    /** سقفِ نرخِ آپلود — سخت‌گیرانه‌تر، چون دیسک می‌خورد. */
    private static final int MAX_UPLOADS = 10;
    private static final long UPLOAD_WINDOW_MS = 300_000;

    private static final int MAX_BODY_LENGTH = 4000;
    private static final int PREVIEW_LENGTH = 80;
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final MongoOperations mongoOperations;
    private final MessageBroadcaster broadcaster;
    private final BusinessHoursService businessHours;

    private final Map<String, Deque<Long>> messageHits = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> uploadHits = new ConcurrentHashMap<>();

    public ChatService(ConversationRepository conversationRepository,
                       ChatMessageRepository messageRepository,
                       MongoOperations mongoOperations,
                       MessageBroadcaster broadcaster,
                       BusinessHoursService businessHours) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.mongoOperations = mongoOperations;
        this.broadcaster = broadcaster;
        this.businessHours = businessHours;
    }

    // ==========================================================
    // گفت‌وگو
    // ==========================================================

    /**
     * گفت‌وگوی همیشگیِ یک مشتری؛ اگر نبود ساخته می‌شود.
     * <p>
     * عمداً «یکی به‌ازای هر مشتری» است: گفت‌وگوی بسته‌شده با پیامِ بعدی دوباره باز
     * می‌شود و تاریخچه پیوسته می‌ماند.
     */
    public Conversation conversationOf(User customer) {
        return conversationRepository.findByCustomerId(customer.getId())
                .orElseGet(() -> {
                    Conversation c = new Conversation();
                    c.setCustomerId(customer.getId());
                    c.setCustomerName(displayName(customer));
                    c.setStatus(ConversationStatus.OPEN);
                    c.setLastMessageAt(Instant.now());
                    return conversationRepository.save(c);
                });
    }

    public Conversation requireConversation(String conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "گفت‌وگو پیدا نشد"));
    }

    /**
     * مرزِ دسترسی — هر اندپوینتِ چت باید از این رد شود.
     * <ul>
     *   <li>مشتری: فقط گفت‌وگوی خودش</li>
     *   <li>کارشناس: گفت‌وگوهای تخصیص‌یافته به خودش، به‌علاوهٔ صفِ تصاحب‌نشده</li>
     *   <li>ادمین: همه</li>
     * </ul>
     */
    public void assertMember(Conversation conversation, User user) {
        if (user.getRole() == Role.ADMIN) return;
        if (user.getId().equals(conversation.getCustomerId())) return;
        if (isAgent(user)) {
            String assignee = conversation.getAssignedAgentId();
            if (assignee == null || assignee.equals(user.getId())) return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "به این گفت‌وگو دسترسی ندارید");
    }

    public boolean isAgent(User user) {
        Role r = user.getRole();
        return r == Role.ADMIN || r == Role.PRICER || r == Role.SALES || r == Role.SUPPORT;
    }

    /** صفِ مشترک: تصاحب‌نشده‌ها، قدیمی‌ترین انتظار اول. */
    public List<Conversation> queue() {
        List<Conversation> open = new ArrayList<>(
                conversationRepository.findByAssignedAgentIdIsNullAndStatus(ConversationStatus.OPEN));
        open.sort(Comparator.comparing(
                Conversation::getLastMessageAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return open;
    }

    public List<Conversation> myChats(String agentId) {
        return conversationRepository.findByAssignedAgentIdOrderByLastMessageAtDesc(agentId);
    }

    /**
     * تصاحبِ اتمیکِ گفت‌وگو.
     * <p>
     * ⚠️ <b>شرطِ مسابقه:</b> دو کارشناس ممکن است هم‌زمان کلیک کنند. این عمداً
     * {@code findAndModify} با پیش‌شرطِ {@code assignedAgentId == null} است و نه
     * «بخوان، بعد بنویس» — آن الگو هر دو را مالک می‌کند و پیام‌ها قاطی می‌شود.
     * مونگو تضمین می‌کند فقط یکی از دو درخواست سند را برمی‌گرداند.
     */
    public Conversation claim(String conversationId, User agent) {
        Conversation claimed = mongoOperations.findAndModify(
                Query.query(Criteria.where("_id").is(conversationId).and("assignedAgentId").is(null)),
                new Update()
                        .set("assignedAgentId", agent.getId())
                        .set("assignedAgentName", displayName(agent))
                        .set("status", ConversationStatus.ASSIGNED),
                FindAndModifyOptions.options().returnNew(true),
                Conversation.class);

        if (claimed != null) {
            // تاریخچه باید خودتوضیح بماند: چه کسی و کِی برش داشت.
            appendSystemMessage(claimed, "گفت‌وگو توسط " + displayName(agent) + " برداشته شد.");
            broadcaster.broadcastToAgents(Map.of("event", "queue"));
            return claimed;
        }

        Conversation existing = requireConversation(conversationId);
        if (agent.getId().equals(existing.getAssignedAgentId())) {
            return existing; // خودش قبلاً برداشته — دوباره‌کلیک نباید خطا بدهد
        }
        throw new ApiException(HttpStatus.CONFLICT,
                "این گفت‌وگو را همکارتان «" + nullSafe(existing.getAssignedAgentName()) + "» برداشت.");
    }

    // ==========================================================
    // پیام
    // ==========================================================

    public ChatMessage sendFromCustomer(User customer, String body, String replyToId,
                                        ChatMessage.Attachment attachment) {
        // قفلِ ساعتِ کاری سمتِ سرور — قفلِ UI به‌تنهایی دور زدنی است.
        if (!businessHours.isOpenNow()) {
            throw new ApiException(HttpStatus.FORBIDDEN, BusinessHoursService.CLOSED_MESSAGE);
        }
        enforceRate(messageHits, customer.getId(), MAX_MESSAGES, MESSAGE_WINDOW_MS,
                "پیام‌ها را کمی آرام‌تر بفرستید.");

        Conversation conversation = conversationOf(customer);
        // مشتری بعد از بسته‌شدن دوباره نوشته: همان گفت‌وگو باز می‌شود، نه گفت‌وگوی دوم.
        if (conversation.getStatus() == ConversationStatus.CLOSED) {
            conversation.setStatus(conversation.getAssignedAgentId() == null
                    ? ConversationStatus.OPEN : ConversationStatus.ASSIGNED);
            conversation.setClosedAt(null);
        }
        boolean wasInQueue = conversation.getAssignedAgentId() == null;

        ChatMessage saved = append(conversation, customer.getId(), SenderRole.CUSTOMER,
                displayName(customer), body, replyToId, attachment);

        conversation.setUnreadForAgent(conversation.getUnreadForAgent() + 1);
        touch(conversation, saved);

        if (wasInQueue) broadcaster.broadcastToAgents(Map.of("event", "queue"));
        return saved;
    }

    public ChatMessage sendFromAgent(User agent, String conversationId, String body, String replyToId,
                                     ChatMessage.Attachment attachment) {
        enforceRate(messageHits, agent.getId(), MAX_MESSAGES, MESSAGE_WINDOW_MS,
                "پیام‌ها را کمی آرام‌تر بفرستید.");

        Conversation conversation = requireConversation(conversationId);
        assertMember(conversation, agent);
        if (conversation.getAssignedAgentId() == null) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "اول گفت‌وگو را بردارید تا برای شما تخصیص یابد.");
        }

        ChatMessage saved = append(conversation, agent.getId(), SenderRole.AGENT,
                displayName(agent), body, replyToId, attachment);

        conversation.setUnreadForCustomer(conversation.getUnreadForCustomer() + 1);
        touch(conversation, saved);
        return saved;
    }

    /**
     * زمینهٔ خودکار: چت از صفحهٔ یک محصول باز شده.
     * <p>
     * این تنها چیزی است که چتِ داخلِ فروشگاه را از یک مسنجرِ عمومی بهتر می‌کند —
     * کارشناس بدونِ پرسیدن می‌داند موضوع چیست.
     */
    public void recordProductContext(Conversation conversation, String productName, String productUrl) {
        String name = plainText(productName);
        if (name == null || name.isBlank()) return;
        String url = plainText(productUrl);
        String text = "مشتری از صفحهٔ محصول «" + name + "» چت را باز کرد."
                + (url == null || url.isBlank() ? "" : " (" + url + ")");
        appendSystemMessage(conversation, text);
    }

    public ChatMessage appendSystemMessage(Conversation conversation, String text) {
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversation.getId());
        message.setSenderRole(SenderRole.SYSTEM);
        message.setType(MessageType.SYSTEM);
        message.setBody(plainText(text));
        ChatMessage saved = messageRepository.save(message);
        broadcaster.broadcast(conversation.getId(),
                Map.of("event", "message", "conversationId", conversation.getId(), "message", toView(saved)));
        return saved;
    }

    private ChatMessage append(Conversation conversation, String senderId, SenderRole senderRole,
                               String senderName, String body, String replyToId,
                               ChatMessage.Attachment attachment) {
        String clean = plainText(body);
        if ((clean == null || clean.isBlank()) && attachment == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "پیامِ خالی نمی‌شود فرستاد");
        }
        if (clean != null && clean.length() > MAX_BODY_LENGTH) {
            clean = clean.substring(0, MAX_BODY_LENGTH);
        }

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversation.getId());
        message.setSenderId(senderId);
        message.setSenderRole(senderRole);
        message.setSenderName(senderName);
        message.setBody(clean);
        message.setAttachment(attachment);
        message.setType(attachment == null ? MessageType.TEXT
                : (ChatAttachmentService.isImage(attachment.getMime()) ? MessageType.IMAGE : MessageType.FILE));
        // فقط ریپلای به پیامی از همین گفت‌وگو؛ وگرنه می‌شد به پیامِ گفت‌وگوی دیگری اشاره داد
        // و کلاینتِ طرفِ مقابل متنِ نقل‌شده‌ای را می‌دید که حقِ دیدنش را ندارد.
        if (replyToId != null && !replyToId.isBlank()) {
            messageRepository.findById(replyToId)
                    .filter(m -> m.getConversationId().equals(conversation.getId()))
                    .ifPresent(m -> message.setReplyToId(m.getId()));
        }
        message.setDeliveredAt(Instant.now());

        ChatMessage saved = messageRepository.save(message);
        broadcaster.broadcast(conversation.getId(),
                Map.of("event", "message", "conversationId", conversation.getId(), "message", toView(saved)));
        return saved;
    }

    private void touch(Conversation conversation, ChatMessage last) {
        conversation.setLastMessageAt(last.getCreatedAt());
        conversation.setLastMessagePreview(preview(last));
        conversationRepository.save(conversation);
    }

    private String preview(ChatMessage message) {
        if (message.getAttachment() != null) {
            return message.getType() == MessageType.IMAGE ? "🖼️ تصویر" : "📎 فایل";
        }
        String body = nullSafe(message.getBody());
        return body.length() > PREVIEW_LENGTH ? body.substring(0, PREVIEW_LENGTH) + "…" : body;
    }

    // ==========================================================
    // تاریخچه
    // ==========================================================

    /** صفحه‌بندیِ اسکرول‌به‌بالا. خروجی صعودی است تا کلاینت مستقیم بالای لیست بچسباندش. */
    public List<ChatMessage> history(String conversationId, String beforeMessageId, Integer limit) {
        int size = (limit == null || limit <= 0 || limit > 200) ? DEFAULT_PAGE_SIZE : limit;
        List<ChatMessage> page;
        if (beforeMessageId == null || beforeMessageId.isBlank()) {
            page = messageRepository.findByConversationIdOrderByCreatedAtDesc(
                    conversationId, PageRequest.of(0, size));
        } else {
            Instant before = messageRepository.findById(beforeMessageId)
                    .map(ChatMessage::getCreatedAt)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "نقطهٔ شروعِ نامعتبر"));
            page = messageRepository.findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                    conversationId, before, PageRequest.of(0, size));
        }
        List<ChatMessage> ascending = new ArrayList<>(page);
        ascending.sort(Comparator.comparing(ChatMessage::getCreatedAt));
        return ascending;
    }

    /**
     * جبرانِ قطعیِ وب‌سوکت. کلاینت بعد از وصلِ دوباره شناسهٔ آخرین پیامِ خودش را
     * می‌دهد و جامانده‌ها را می‌گیرد — به همین دلیل به وب‌سوکت برایِ تحویلِ تضمینی
     * تکیه نشده است.
     */
    public List<ChatMessage> messagesAfter(String conversationId, String afterMessageId) {
        if (afterMessageId == null || afterMessageId.isBlank()) {
            return history(conversationId, null, DEFAULT_PAGE_SIZE);
        }
        Instant after = messageRepository.findById(afterMessageId)
                .map(ChatMessage::getCreatedAt)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "نقطهٔ شروعِ نامعتبر"));
        return messageRepository.findByConversationIdAndCreatedAtAfterOrderByCreatedAtAsc(conversationId, after);
    }

    /** تیکِ خوانده‌شده + صفرکردنِ شمارندهٔ همان سمت. */
    public void markRead(Conversation conversation, boolean readerIsAgent) {
        mongoOperations.updateMulti(
                Query.query(Criteria.where("conversationId").is(conversation.getId())
                        .and("senderRole").is(readerIsAgent ? SenderRole.CUSTOMER : SenderRole.AGENT)
                        .and("readAt").is(null)),
                new Update().set("readAt", Instant.now()),
                ChatMessage.class);

        if (readerIsAgent) conversation.setUnreadForAgent(0);
        else conversation.setUnreadForCustomer(0);
        conversationRepository.save(conversation);

        broadcaster.broadcast(conversation.getId(), Map.of(
                "event", "read",
                "conversationId", conversation.getId(),
                "by", readerIsAgent ? "AGENT" : "CUSTOMER"));
    }

    public void checkUploadRate(String userId) {
        enforceRate(uploadHits, userId, MAX_UPLOADS, UPLOAD_WINDOW_MS,
                "تعدادِ فایل‌های ارسالی زیاد است. کمی بعد دوباره تلاش کنید.");
    }

    // ==========================================================
    // نماها
    // ==========================================================

    /**
     * نمایِ پیام برای کلاینت.
     * <p>
     * 🔴 {@code storedName} عمداً بیرون نمی‌رود و آدرسِ ضمیمه از روی شناسهٔ پیام
     * ساخته می‌شود — تنها راهِ گرفتنِ فایل، کنترلری است که عضویت را چک می‌کند.
     */
    public Map<String, Object> toView(ChatMessage message) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", message.getId());
        out.put("conversationId", message.getConversationId());
        out.put("senderId", message.getSenderId());
        out.put("senderRole", message.getSenderRole());
        out.put("senderName", message.getSenderName());
        out.put("type", message.getType());
        out.put("body", message.getBody());
        out.put("replyToId", message.getReplyToId());
        out.put("createdAt", message.getCreatedAt());
        out.put("readAt", message.getReadAt());
        if (message.getAttachment() != null) {
            ChatMessage.Attachment a = message.getAttachment();
            Map<String, Object> att = new LinkedHashMap<>();
            att.put("url", "/api/v1/chat/attachments/" + message.getId());
            att.put("name", a.getName());
            att.put("sizeBytes", a.getSizeBytes());
            att.put("mime", a.getMime());
            att.put("durationMs", a.getDurationMs());
            out.put("attachment", att);
        }
        return out;
    }

    public Map<String, Object> toView(Conversation conversation) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", conversation.getId());
        out.put("customerId", conversation.getCustomerId());
        out.put("customerName", conversation.getCustomerName());
        out.put("assignedAgentId", conversation.getAssignedAgentId());
        out.put("assignedAgentName", conversation.getAssignedAgentName());
        out.put("status", conversation.getStatus());
        out.put("lastMessageAt", conversation.getLastMessageAt());
        out.put("lastMessagePreview", conversation.getLastMessagePreview());
        out.put("unreadForAgent", conversation.getUnreadForAgent());
        out.put("unreadForCustomer", conversation.getUnreadForCustomer());
        out.put("createdAt", conversation.getCreatedAt());
        return out;
    }

    public String displayName(User user) {
        String full = (nullSafe(user.getFirstName()) + " " + nullSafe(user.getLastName())).trim();
        return full.isEmpty() ? nullSafe(user.getUsername()) : full;
    }

    // ==========================================================
    // کمکی
    // ==========================================================

    /**
     * ورودیِ کاربر به متنِ ساده تبدیل می‌شود.
     * <p>
     * ⚠️ جدی‌ترین سطحِ حملهٔ این فیچر: پیامِ مشتری در پنلِ داخلیِ کارشناس رندر می‌شود،
     * پس XSSِ سمتِ مشتری مستقیم به پنل می‌رسد. اینجا تگ‌ها حذف می‌شوند و بعد
     * انتیتی‌ها به متنِ خام برمی‌گردند تا چیزی که ذخیره می‌شود «متن» باشد نه HTML؛
     * سمتِ کلاینت هم فقط با {@code textContent} رندر می‌شود. دو لایه، عمداً.
     */
    private String plainText(String raw) {
        if (raw == null) return null;
        return Parser.unescapeEntities(SecurityUtils.clean(raw), false);
    }

    private void enforceRate(Map<String, Deque<Long>> hits, String userId,
                             int max, long windowMs, String message) {
        long now = System.currentTimeMillis();
        Deque<Long> stamps = hits.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (stamps) {
            while (!stamps.isEmpty() && now - stamps.peekFirst() > windowMs) {
                stamps.pollFirst();
            }
            if (stamps.size() >= max) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, message);
            }
            stamps.addLast(now);
        }
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }
}
