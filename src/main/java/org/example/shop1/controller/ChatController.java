package org.example.shop1.controller;

import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.ChatMessage;
import org.example.shop1.model.entity.Conversation;
import org.example.shop1.model.entity.User;
import org.example.shop1.model.enums.ConversationStatus;
import org.example.shop1.model.enums.Role;
import org.example.shop1.model.reposritory.ChatMessageRepository;
import org.example.shop1.model.reposritory.UserRepository;
import org.example.shop1.model.service.BusinessHoursService;
import org.example.shop1.model.service.ChatAttachmentService;
import org.example.shop1.model.service.ChatService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * APIِ چتِ پشتیبانی.
 * <p>
 * همهٔ مسیرها زیرِ {@code /api/v1/chat/**} احرازِ هویت می‌خواهند (SecurityConfig)، و
 * علاوه بر آن هر اندپوینت <b>عضویت</b> را هم چک می‌کند: مشتری فقط گفت‌وگوی خودش،
 * کارشناس فقط تخصیص‌یافته‌ها به‌علاوهٔ صفِ تصاحب‌نشده، ادمین همه.
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;
    private final ChatAttachmentService attachmentService;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final BusinessHoursService businessHours;

    public ChatController(ChatService chatService, ChatAttachmentService attachmentService,
                          ChatMessageRepository messageRepository, UserRepository userRepository,
                          BusinessHoursService businessHours) {
        this.chatService = chatService;
        this.attachmentService = attachmentService;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.businessHours = businessHours;
    }

    // ==========================================================
    // نشستِ کاربر
    // ==========================================================

    /** وضعیتِ اولیه: کاربر کارشناس است یا مشتری، گفت‌وگویش کدام است، و ساعتِ کاری چه می‌گوید. */
    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> session(Authentication authentication) {
        User user = currentUser(authentication);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("userId", user.getId());
        out.put("displayName", chatService.displayName(user));
        out.put("isAgent", chatService.isAgent(user));
        out.put("businessHours", businessHours.status());
        // کارشناس گفت‌وگوی مشتریِ خودش را ندارد؛ از صف/چت‌های من کار می‌کند.
        if (!chatService.isAgent(user)) {
            out.put("conversation", chatService.toView(chatService.conversationOf(user)));
        }
        return ResponseEntity.ok(out);
    }

    /**
     * زمینهٔ خودکارِ محصول — وقتی حباب از صفحهٔ یک محصول باز می‌شود.
     * فقط مشتری؛ کارشناس چنین چیزی ثبت نمی‌کند.
     */
    @PostMapping("/context")
    public ResponseEntity<Void> productContext(@RequestBody Map<String, String> body,
                                               Authentication authentication) {
        User user = currentUser(authentication);
        if (chatService.isAgent(user)) return ResponseEntity.noContent().build();
        Conversation conversation = chatService.conversationOf(user);
        chatService.recordProductContext(conversation, body.get("productName"), body.get("productUrl"));
        return ResponseEntity.noContent().build();
    }

    // ==========================================================
    // صف و تصاحب (کارشناس)
    // ==========================================================

    @GetMapping("/conversations/queue")
    public ResponseEntity<List<Map<String, Object>>> queue(Authentication authentication) {
        requireAgent(currentUser(authentication));
        return ResponseEntity.ok(chatService.queue().stream().map(chatService::toView).toList());
    }

    @GetMapping("/conversations/mine")
    public ResponseEntity<List<Map<String, Object>>> mine(Authentication authentication) {
        User agent = requireAgent(currentUser(authentication));
        return ResponseEntity.ok(chatService.myChats(agent.getId()).stream().map(chatService::toView).toList());
    }

    /** تصاحب — اتمیک؛ بازندهٔ مسابقه ۴۰۹ با پیامِ روشن می‌گیرد. */
    @PostMapping("/conversations/{id}/claim")
    public ResponseEntity<Map<String, Object>> claim(@PathVariable String id, Authentication authentication) {
        User agent = requireAgent(currentUser(authentication));
        return ResponseEntity.ok(chatService.toView(chatService.claim(id, agent)));
    }

    @PostMapping("/conversations/{id}/close")
    public ResponseEntity<Map<String, Object>> close(@PathVariable String id, Authentication authentication) {
        User agent = requireAgent(currentUser(authentication));
        Conversation conversation = chatService.requireConversation(id);
        chatService.assertMember(conversation, agent);
        conversation.setStatus(ConversationStatus.CLOSED);
        conversation.setClosedAt(Instant.now());
        chatService.appendSystemMessage(conversation, "گفت‌وگو توسط " + chatService.staffDisplayName(agent) + " بسته شد.");
        return ResponseEntity.ok(chatService.toView(conversation));
    }

    // ==========================================================
    // پیام‌ها
    // ==========================================================

    /**
     * تاریخچه. {@code before} برای اسکرول به بالا، {@code after} برای جبرانِ قطعیِ
     * وب‌سوکت بعد از وصلِ دوباره.
     */
    @GetMapping("/messages")
    public ResponseEntity<List<Map<String, Object>>> messages(
            @RequestParam(required = false) String conversationId,
            @RequestParam(required = false) String before,
            @RequestParam(required = false) String after,
            @RequestParam(required = false) Integer limit,
            Authentication authentication) {

        User user = currentUser(authentication);
        Conversation conversation = resolveConversation(conversationId, user);

        List<ChatMessage> found = (after != null && !after.isBlank())
                ? chatService.messagesAfter(conversation.getId(), after)
                : chatService.history(conversation.getId(), before, limit);

        return ResponseEntity.ok(found.stream().map(chatService::toView).toList());
    }

    @PostMapping("/messages")
    public ResponseEntity<Map<String, Object>> send(@RequestBody Map<String, String> body,
                                                    Authentication authentication) {
        User user = currentUser(authentication);
        String text = body.get("body");
        String replyToId = body.get("replyToId");

        ChatMessage saved = chatService.isAgent(user)
                ? chatService.sendFromAgent(user, requireId(body.get("conversationId")), text, replyToId, null)
                : chatService.sendFromCustomer(user, text, replyToId, null);

        return ResponseEntity.ok(chatService.toView(saved));
    }

    /** آپلودِ ضمیمه — همان مسیرِ ارسالِ پیام، با فایل. */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                                      @RequestParam(required = false) String conversationId,
                                                      @RequestParam(required = false) String body,
                                                      @RequestParam(required = false) String replyToId,
                                                      Authentication authentication) {
        User user = currentUser(authentication);
        chatService.checkUploadRate(user.getId());
        ChatMessage.Attachment attachment = attachmentService.store(file);

        ChatMessage saved = chatService.isAgent(user)
                ? chatService.sendFromAgent(user, requireId(conversationId), body, replyToId, attachment)
                : chatService.sendFromCustomer(user, body, replyToId, attachment);

        return ResponseEntity.ok(chatService.toView(saved));
    }

    @PostMapping("/conversations/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable String id, Authentication authentication) {
        User user = currentUser(authentication);
        Conversation conversation = chatService.requireConversation(id);
        chatService.assertMember(conversation, user);
        chatService.markRead(conversation, chatService.isAgent(user));
        return ResponseEntity.noContent().build();
    }

    // ==========================================================
    // ضمیمه
    // ==========================================================

    /**
     * 🔴 تنها راهِ گرفتنِ ضمیمهٔ چت.
     * <p>
     * فایل‌ها در {@code /opt/shop/chat/} هستند که نه {@code ResourceHandler} داریم
     * برایش نه {@code location} در nginx — پس آدرسِ مستقیم وجود ندارد و هرکس فایل
     * می‌خواهد باید از همین‌جا و با چکِ عضویت رد شود. حدس‌زدنِ نامِ UUID هم فایده
     * ندارد، چون کلید عضویت است نه نام.
     */
    @GetMapping("/attachments/{messageId}")
    public ResponseEntity<Resource> attachment(@PathVariable String messageId, Authentication authentication) {
        User user = currentUser(authentication);
        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "پیام پیدا نشد"));
        if (message.getAttachment() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "این پیام ضمیمه ندارد");
        }
        chatService.assertMember(chatService.requireConversation(message.getConversationId()), user);

        ChatMessage.Attachment a = message.getAttachment();
        byte[] bytes = attachmentService.read(a);

        // غیرتصویری‌ها اجباراً دانلود می‌شوند تا در دامنهٔ ما رندر/اجرا نشوند.
        String disposition = (ChatAttachmentService.isImage(a.getMime()) ? "inline" : "attachment")
                + "; filename*=UTF-8''" + java.net.URLEncoder.encode(
                        a.getName() == null ? "attachment" : a.getName(), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header("X-Content-Type-Options", "nosniff")
                // فایلِ خصوصیِ یک گفت‌وگو نباید در کشِ مشترک بنشیند.
                .cacheControl(CacheControl.noStore().cachePrivate())
                .contentType(MediaType.parseMediaType(
                        a.getMime() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : a.getMime()))
                .body(new ByteArrayResource(bytes));
    }

    // ==========================================================
    // کمکی
    // ==========================================================

    private User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "ابتدا وارد شوید");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "کاربر یافت نشد"));
    }

    private User requireAgent(User user) {
        if (!chatService.isAgent(user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "این بخش مخصوصِ کارشناسان است");
        }
        return user;
    }

    /** مشتری شناسهٔ گفت‌وگو نمی‌فرستد؛ همیشه گفت‌وگوی خودش است. کارشناس باید بفرستد. */
    private Conversation resolveConversation(String conversationId, User user) {
        if (conversationId == null || conversationId.isBlank()) {
            if (chatService.isAgent(user) && user.getRole() != Role.USER) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "شناسهٔ گفت‌وگو لازم است");
            }
            return chatService.conversationOf(user);
        }
        Conversation conversation = chatService.requireConversation(conversationId);
        chatService.assertMember(conversation, user);
        return conversation;
    }

    private String requireId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "شناسهٔ گفت‌وگو لازم است");
        }
        return conversationId;
    }
}
