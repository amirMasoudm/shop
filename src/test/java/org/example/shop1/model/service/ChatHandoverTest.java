package org.example.shop1.model.service;

import org.bson.Document;
import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.ChatMessage;
import org.example.shop1.model.entity.Conversation;
import org.example.shop1.model.entity.User;
import org.example.shop1.model.enums.ConversationStatus;
import org.example.shop1.model.enums.Role;
import org.example.shop1.model.enums.SenderRole;
import org.example.shop1.model.reposritory.ChatMessageRepository;
import org.example.shop1.model.reposritory.ConversationRepository;
import org.example.shop1.model.service.analytics.UserEventRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * نگهبانِ «ارجاع به کارشناسِ دیگر» و «انصراف از برداشت».
 * <p>
 * چهار چیز اینجا آزموده می‌شود که شکستنشان بی‌صدا است:
 * <ul>
 *   <li><b>مرزِ «بی‌جواب» کجاست</b> — فقط پیام‌هایِ مشتری بعد از آخرین پاسخِ کارشناس
 *       باید به نخوانده برگردند. اگر مرز اشتباه بیفتد، یا پیامِ جواب‌داده دوباره
 *       نخوانده می‌شود، یا پیامِ رهاشده بی‌صدا سین‌خورده می‌ماند و کارشناسِ بعدی
 *       اصلاً نمی‌بیندش — یعنی دقیقاً همان چیزی که این فیچر برایش ساخته شد.</li>
 *   <li><b>پیامِ SYSTEM پاسخ نیست</b> — «گفت‌وگو برداشته شد» جوابِ مشتری نبوده؛
 *       اگر به‌عنوان مرز حساب شود، برداشتنِ ساده همهٔ بدهیِ قبلی را پاک می‌کند.</li>
 *   <li><b>«نه» واقعاً یعنی نه</b> — وقتی کارشناس تیک را برمی‌دارد، هیچ readAtی
 *       نباید پاک شود.</li>
 *   <li><b>فقط دارنده حق دارد</b> — وگرنه هر کارشناسی می‌تواند گفت‌وگوی دستِ
 *       همکارش را از او بگیرد.</li>
 * </ul>
 */
class ChatHandoverTest {

    private static final String CONV = "conv-1";

    private final Map<String, Conversation> conversations = new LinkedHashMap<>();
    private final Map<String, ChatMessage> messages = new LinkedHashMap<>();

    private ChatService service;
    private User holder;
    private User other;
    private User customer;

    // ==========================================================
    // چیدمان
    // ==========================================================

    @BeforeEach
    void setUp() {
        ConversationRepository conversationRepo = mock(ConversationRepository.class);
        ChatMessageRepository messageRepo = mock(ChatMessageRepository.class);
        MongoOperations mongo = mock(MongoOperations.class);

        service = new ChatService(conversationRepo, messageRepo, mongo,
                mock(MessageBroadcaster.class), mock(BusinessHoursService.class),
                mock(UserEventRecorder.class));

        holder = staff("agent-1", "الف", Role.SALES);
        other = staff("agent-2", "ب", Role.SUPPORT);
        customer = staff("cust-1", "مشتری", Role.USER);

        when(conversationRepo.findById(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(conversations.get(inv.getArgument(0, String.class))));
        when(conversationRepo.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            conversations.put(c.getId(), c);
            return c;
        });
        when(conversationRepo.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenAnswer(inv -> new org.springframework.data.domain.PageImpl<>(
                        new ArrayList<>(conversations.values())));
        when(messageRepo.save(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            if (m.getId() == null) m.setId("sys-" + messages.size());
            messages.put(m.getId(), m);
            return m;
        });

        // ── کوئری‌هایی که منطقِ «بی‌جواب» رویشان سوار است ──
        when(messageRepo.findByConversationIdAndSenderRoleOrderByCreatedAtDesc(anyString(), any(), any()))
                .thenAnswer(inv -> {
                    List<ChatMessage> hit = ofRole(inv.getArgument(0), inv.getArgument(1));
                    hit.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                    return hit.isEmpty() ? List.of() : List.of(hit.get(0));
                });
        when(messageRepo.findByConversationIdAndSenderRoleOrderByCreatedAtAsc(anyString(), any()))
                .thenAnswer(inv -> {
                    List<ChatMessage> hit = ofRole(inv.getArgument(0), inv.getArgument(1));
                    hit.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
                    return hit;
                });
        when(messageRepo.findByConversationIdAndSenderRoleAndCreatedAtAfterOrderByCreatedAtAsc(
                anyString(), any(), any(Instant.class)))
                .thenAnswer(inv -> {
                    Instant after = inv.getArgument(2);
                    List<ChatMessage> hit = new ArrayList<>(ofRole(inv.getArgument(0), inv.getArgument(1)));
                    hit.removeIf(m -> !m.getCreatedAt().isAfter(after));
                    hit.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
                    return hit;
                });

        // ── مونگوی ساختگی، ولی با همان قیدها: پیش‌شرطِ findAndModify واقعاً چک می‌شود ──
        when(mongo.findAndModify(any(Query.class), any(Update.class), any(), eq(Conversation.class)))
                .thenAnswer(inv -> applyFindAndModify(inv.getArgument(0), inv.getArgument(1)));
        when(mongo.updateMulti(any(Query.class), any(Update.class), eq(ChatMessage.class)))
                .thenAnswer(inv -> { applyUnreadReset(inv.getArgument(0), inv.getArgument(1)); return null; });
    }

    private List<ChatMessage> ofRole(String conversationId, SenderRole role) {
        List<ChatMessage> hit = new ArrayList<>();
        for (ChatMessage m : messages.values()) {
            if (conversationId.equals(m.getConversationId()) && m.getSenderRole() == role) hit.add(m);
        }
        return hit;
    }

    /**
     * فقط همان چیزی که سرویس واقعاً به مونگو می‌دهد اجرا می‌شود: پیش‌شرطِ
     * {@code assignedAgentId} چک می‌شود و اگر نخورد {@code null} برمی‌گردد — همان
     * رفتاری که سرویس برایش ۴۰۹ می‌دهد.
     */
    private Conversation applyFindAndModify(Query query, Update update) {
        Document where = query.getQueryObject();
        Conversation c = conversations.get(where.getString("_id"));
        if (c == null) return null;
        if (where.containsKey("assignedAgentId")
                && !java.util.Objects.equals(where.get("assignedAgentId"), c.getAssignedAgentId())) {
            return null;
        }
        Document u = update.getUpdateObject();
        Document set = (Document) u.get("$set");
        if (set != null) {
            if (set.containsKey("assignedAgentId")) c.setAssignedAgentId(set.getString("assignedAgentId"));
            if (set.containsKey("assignedAgentName")) c.setAssignedAgentName(set.getString("assignedAgentName"));
            if (set.containsKey("status")) c.setStatus((ConversationStatus) set.get("status"));
        }
        Document unset = (Document) u.get("$unset");
        if (unset != null) {
            if (unset.containsKey("assignedAgentId")) c.setAssignedAgentId(null);
            if (unset.containsKey("assignedAgentName")) c.setAssignedAgentName(null);
            if (unset.containsKey("closedAt")) c.setClosedAt(null);
        }
        return c;
    }

    @SuppressWarnings("unchecked")
    private void applyUnreadReset(Query query, Update update) {
        Document where = query.getQueryObject();
        Document idClause = (Document) where.get("_id");
        List<String> ids = (List<String>) idClause.get("$in");
        Document unset = (Document) update.getUpdateObject().get("$unset");
        assertNotNull(unset, "برگرداندن به نخوانده باید readAt را unset کند");
        assertTrue(unset.containsKey("readAt"));
        ids.forEach(id -> messages.get(id).setReadAt(null));
    }

    // ==========================================================
    // دادهٔ آزمون
    // ==========================================================

    private User staff(String id, String name, Role role) {
        User u = new User();
        u.setId(id);
        u.setFirstName(name);
        u.setLastName("");
        u.setUsername(id);
        u.setRole(role);
        return u;
    }

    /** گفت‌وگویی که دستِ {@code holder} است. */
    private Conversation assignedConversation() {
        Conversation c = new Conversation();
        c.setId(CONV);
        c.setCustomerId(customer.getId());
        c.setCustomerName("مشتری");
        c.setAssignedAgentId(holder.getId());
        c.setAssignedAgentName("الف");
        c.setStatus(ConversationStatus.ASSIGNED);
        c.setLastMessageAt(Instant.now());
        conversations.put(CONV, c);
        return c;
    }

    private ChatMessage msg(String id, SenderRole role, int minutesAgo, boolean read) {
        ChatMessage m = new ChatMessage();
        m.setId(id);
        m.setConversationId(CONV);
        m.setSenderRole(role);
        m.setCreatedAt(Instant.now().minusSeconds(minutesAgo * 60L));
        m.setBody(id);
        if (read) m.setReadAt(Instant.now());
        messages.put(id, m);
        return m;
    }

    // ==========================================================
    // آزمون‌ها
    // ==========================================================

    @Test
    void releasePutsConversationBackInTheQueue() {
        Conversation c = assignedConversation();

        ChatService.Handover result = service.release(CONV, holder, false);

        assertNull(result.conversation().getAssignedAgentId(), "بعد از انصراف باید بی‌صاحب شود");
        assertNull(result.conversation().getAssignedAgentName());
        assertEquals(ConversationStatus.OPEN, result.conversation().getStatus(), "باید به صف برگردد");
        assertEquals(0, result.restoredUnread());
        assertSame(c, conversations.get(CONV));
    }

    @Test
    void transferMovesConversationToTheOtherAgent() {
        assignedConversation();

        ChatService.Handover result = service.transfer(CONV, holder, other, false);

        assertEquals(other.getId(), result.conversation().getAssignedAgentId());
        assertEquals(ConversationStatus.ASSIGNED, result.conversation().getStatus());
        assertTrue(messages.values().stream()
                        .anyMatch(m -> m.getSenderRole() == SenderRole.SYSTEM
                                && m.getBody() != null && m.getBody().contains("ارجاع شد")),
                "تاریخچه باید خودتوضیح بماند: ارجاع باید پیامِ سیستمی بگذارد");
    }

    /**
     * قلبِ فیچر: فقط پیام‌هایِ بعد از آخرین پاسخِ کارشناس نخوانده می‌شوند.
     * پیامِ قبل از آن پاسخ داده شده و نباید دوباره بالا بیاید.
     */
    @Test
    void restoreUnreadTouchesOnlyMessagesAfterTheLastAgentReply() {
        assignedConversation();
        msg("c1", SenderRole.CUSTOMER, 50, true);   // جواب داده شده
        msg("a1", SenderRole.AGENT, 40, false);     // ← مرز
        msg("c2", SenderRole.CUSTOMER, 30, true);   // سین‌خورده، بی‌جواب
        msg("c3", SenderRole.CUSTOMER, 20, true);   // سین‌خورده، بی‌جواب

        ChatService.Handover result = service.release(CONV, holder, true);

        assertEquals(2, result.restoredUnread());
        assertNotNull(messages.get("c1").getReadAt(), "پیامِ جواب‌داده نباید نخوانده شود");
        assertNull(messages.get("c2").getReadAt());
        assertNull(messages.get("c3").getReadAt());
        assertEquals(2, conversations.get(CONV).getUnreadForAgent(), "نشانِ نخوانده باید همان دو باشد");
    }

    /** پیامِ سیستمیِ «برداشته شد» پاسخ نیست و نباید مرز شود. */
    @Test
    void systemMessageDoesNotCountAsAnAnswer() {
        assignedConversation();
        msg("c1", SenderRole.CUSTOMER, 50, true);
        msg("s1", SenderRole.SYSTEM, 45, false);    // «گفت‌وگو برداشته شد»
        msg("c2", SenderRole.CUSTOMER, 40, true);

        ChatService.Handover result = service.release(CONV, holder, true);

        assertEquals(2, result.restoredUnread(), "هیچ کارشناسی جواب نداده، پس هر دو بی‌جواب‌اند");
        assertNull(messages.get("c1").getReadAt());
        assertNull(messages.get("c2").getReadAt());
    }

    @Test
    void decliningTheQuestionLeavesEverythingRead() {
        assignedConversation();
        msg("c1", SenderRole.CUSTOMER, 30, true);
        msg("c2", SenderRole.CUSTOMER, 20, true);

        ChatService.Handover result = service.transfer(CONV, holder, other, false);

        assertEquals(0, result.restoredUnread());
        assertNotNull(messages.get("c1").getReadAt(), "وقتی کارشناس «نه» گفت، هیچ‌چیز نباید آن‌سین شود");
        assertNotNull(messages.get("c2").getReadAt());
        assertEquals(0, conversations.get(CONV).getUnreadForAgent());
    }

    @Test
    void nothingPendingWhenTheAgentAlreadyReplied() {
        assignedConversation();
        msg("c1", SenderRole.CUSTOMER, 30, true);
        msg("a1", SenderRole.AGENT, 20, false);

        assertEquals(0, service.unansweredCustomerMessages(CONV).size());
        assertEquals(0, service.release(CONV, holder, true).restoredUnread());
    }

    @Test
    void onlyTheHolderCanHandOver() {
        assignedConversation();

        ApiException transferDenied = assertThrows(ApiException.class,
                () -> service.transfer(CONV, other, holder, false));
        ApiException releaseDenied = assertThrows(ApiException.class,
                () -> service.release(CONV, other, false));

        assertEquals(403, transferDenied.getStatus().value());
        assertEquals(403, releaseDenied.getStatus().value());
        assertEquals(holder.getId(), conversations.get(CONV).getAssignedAgentId(), "مالکیت نباید عوض شده باشد");
    }

    /** ادمین از طرفِ دارنده اجازه دارد — برای وقتی کارشناسی در دسترس نیست. */
    @Test
    void adminMayHandOverSomeoneElsesConversation() {
        assignedConversation();
        User admin = staff("admin-1", "مدیر", Role.ADMIN);

        ChatService.Handover result = service.transfer(CONV, admin, other, false);

        assertEquals(other.getId(), result.conversation().getAssignedAgentId());
    }

    @Test
    void cannotHandOverAConversationNobodyHasClaimed() {
        Conversation c = assignedConversation();
        c.setAssignedAgentId(null);
        c.setAssignedAgentName(null);
        c.setStatus(ConversationStatus.OPEN);

        ApiException e = assertThrows(ApiException.class, () -> service.release(CONV, holder, false));
        assertEquals(409, e.getStatus().value());
    }

    @Test
    void transferToTheSameAgentIsRejected() {
        assignedConversation();

        ApiException e = assertThrows(ApiException.class, () -> service.transfer(CONV, holder, holder, false));
        assertEquals(400, e.getStatus().value());
    }

    // ==========================================================
    // اختیاراتِ مدیر
    // ==========================================================

    /** مدیر باید بتواند گفت‌وگوی توی صف را مستقیم به کسی بدهد، بی‌آنکه اول خودش برش دارد. */
    @Test
    void adminMayAssignAnUnclaimedConversation() {
        Conversation c = assignedConversation();
        c.setAssignedAgentId(null);
        c.setAssignedAgentName(null);
        c.setStatus(ConversationStatus.OPEN);
        User admin = staff("admin-1", "مدیر", Role.ADMIN);

        ChatService.Handover result = service.transfer(CONV, admin, other, false);

        assertEquals(other.getId(), result.conversation().getAssignedAgentId());
        assertEquals(ConversationStatus.ASSIGNED, result.conversation().getStatus());
    }

    /** ولی کارشناسِ عادی نه — وگرنه «برداشتن» و قیدِ اتمیکش دور زده می‌شد. */
    @Test
    void nonAdminCannotAssignAnUnclaimedConversation() {
        Conversation c = assignedConversation();
        c.setAssignedAgentId(null);
        c.setStatus(ConversationStatus.OPEN);

        ApiException e = assertThrows(ApiException.class, () -> service.transfer(CONV, other, holder, false));
        assertEquals(409, e.getStatus().value());
    }

    /** خواستهٔ صریحِ مالک: مدیر بتواند گفت‌وگوی همکار را به <b>خودش</b> بدهد. */
    @Test
    void adminMayTakeSomeoneElsesConversation() {
        assignedConversation();
        User admin = staff("admin-1", "مدیر", Role.ADMIN);

        ChatService.Handover result = service.transfer(CONV, admin, admin, false);

        assertEquals(admin.getId(), result.conversation().getAssignedAgentId());
        assertEquals(ConversationStatus.ASSIGNED, result.conversation().getStatus());
    }

    /**
     * 🔴 نظارت نباید کارِ کارشناس را خراب کند: بازکردنِ گفت‌وگو توسطِ کسی که
     * دارندهٔ آن نیست، نباید «خوانده شد» ثبت کند و نشانِ نخواندهٔ او را صفر کند.
     */
    @Test
    void watchingDoesNotClearTheAssigneesUnreadBadge() {
        Conversation c = assignedConversation();
        User admin = staff("admin-1", "مدیر", Role.ADMIN);

        assertTrue(service.shouldMarkRead(c, holder), "دارنده که باز می‌کند، خوانده می‌شود");
        assertFalse(service.shouldMarkRead(c, admin), "مدیرِ ناظر نباید سین بزند");
        assertFalse(service.shouldMarkRead(c, other), "کارشناسِ غیرِدارنده هم همین‌طور");
        assertTrue(service.shouldMarkRead(c, customer), "مشتری همیشه");
    }

    /** جست‌وجوی نمای مدیر عمداً در جاوا است؛ باید روی نام، کارشناس و پیش‌نمایش کار کند. */
    @Test
    void adminSearchMatchesNameAgentAndPreview() {
        Conversation a = assignedConversation();
        a.setCustomerName("رضا کاظمی");
        a.setAssignedAgentName("خانم سمیعی");
        a.setLastMessagePreview("قیمت روتر");

        assertEquals(1, service.allConversations("کاظمی", 0).size());
        assertEquals(1, service.allConversations("سمیعی", 0).size());
        assertEquals(1, service.allConversations("روتر", 0).size());
        assertEquals(0, service.allConversations("هیچ‌چیز", 0).size());
        assertEquals(1, service.allConversations("", 0).size(), "بی‌جست‌وجو یعنی همه");
    }

    @Test
    void transferToANonAgentIsRejected() {
        assignedConversation();

        ApiException e = assertThrows(ApiException.class, () -> service.transfer(CONV, holder, customer, false));
        assertEquals(400, e.getStatus().value());
    }
}
