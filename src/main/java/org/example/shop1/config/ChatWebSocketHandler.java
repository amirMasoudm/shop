package org.example.shop1.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.shop1.model.entity.Conversation;
import org.example.shop1.model.entity.User;
import org.example.shop1.model.reposritory.UserRepository;
import org.example.shop1.model.service.ChatService;
import org.example.shop1.model.service.LocalMessageBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

/**
 * وب‌سوکتِ خامِ چت — بدونِ STOMP، بدونِ SockJS، بدونِ بروکر (تصمیمِ بندِ ۳ سندِ معماری).
 * <p>
 * پروتکل عمداً کوچک است. از کلاینت فقط دو کنش می‌آید:
 * <ul>
 *   <li>{@code {"action":"subscribe","conversationId":"..."}} — عضویت چک می‌شود، بعد
 *       اتصال به اتاقِ همان گفت‌وگو اضافه می‌شود.</li>
 *   <li>{@code {"action":"typing","conversationId":"..."}} — رویدادِ گذرا، ذخیره نمی‌شود.</li>
 * </ul>
 * <b>ارسالِ پیام عمداً از اینجا نمی‌گذرد</b> و REST است: پیام باید اول در دیتابیس
 * بنشیند (منبعِ حقیقت) و تازه بعد پخش شود. اگر ارسال هم روی سوکت بود، قطعیِ اتصال
 * وسطِ کار یعنی پیامی که کاربر فرستاده ولی هیچ‌جا ثبت نشده.
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final LocalMessageBroadcaster broadcaster;
    private final ChatService chatService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ChatWebSocketHandler(LocalMessageBroadcaster broadcaster, ChatService chatService,
                                UserRepository userRepository, ObjectMapper objectMapper) {
        this.broadcaster = broadcaster;
        this.chatService = chatService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        User user = currentUser(session);
        if (user == null) {
            close(session);
            return;
        }
        // کارشناس‌ها بدونِ بازکردنِ گفت‌وگو هم باید تازه‌شدنِ صف را بگیرند.
        if (chatService.isAgent(user)) {
            broadcaster.registerAgent(session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        User user = currentUser(session);
        if (user == null) {
            close(session);
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            String action = node.path("action").asText("");
            String conversationId = node.path("conversationId").asText(null);
            if (conversationId == null || conversationId.isBlank()) return;

            // هر دو کنش عضویت می‌خواهند؛ بدونِ این، هرکسی می‌توانست با حدسِ شناسه
            // در اتاقِ گفت‌وگوی دیگران بنشیند و پیام‌هایشان را زنده بگیرد.
            Conversation conversation = chatService.requireConversation(conversationId);
            chatService.assertMember(conversation, user);

            switch (action) {
                case "subscribe" -> {
                    broadcaster.join(conversationId, session);
                    send(session, Map.of("event", "subscribed", "conversationId", conversationId));
                }
                case "typing" -> broadcaster.broadcast(conversationId, Map.of(
                        "event", "typing",
                        "conversationId", conversationId,
                        "senderId", user.getId(),
                        "senderName", chatService.displayName(user)));
                default -> { /* کنشِ ناشناس عمداً بی‌صدا نادیده گرفته می‌شود */ }
            }
        } catch (Exception e) {
            log.debug("پاکتِ نامعتبرِ چت: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        broadcaster.remove(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        broadcaster.remove(session);
    }

    private User currentUser(WebSocketSession session) {
        Object username = session.getAttributes().get(ChatHandshakeInterceptor.ATTR_USERNAME);
        if (username == null) return null;
        return userRepository.findByUsername(String.valueOf(username)).orElse(null);
    }

    private void send(WebSocketSession session, Map<String, Object> payload) {
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            }
        } catch (Exception e) {
            log.debug("ارسال به سشنِ چت ناموفق بود: {}", e.getMessage());
        }
    }

    private void close(WebSocketSession session) {
        try {
            session.close(CloseStatus.NOT_ACCEPTABLE);
        } catch (Exception ignored) {
            // اتصال از قبل بسته است
        }
    }
}
