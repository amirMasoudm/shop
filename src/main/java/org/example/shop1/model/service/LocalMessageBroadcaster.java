package org.example.shop1.model.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * پیاده‌سازیِ درون‌حافظه‌ایِ {@link MessageBroadcaster} — <b>تک‌نودی</b>.
 * <p>
 * رجیستری یک {@code Map<conversationId, Set<WebSocketSession>>} است. برای تک‌نود
 * کافی و ساده است؛ محدودیتش در توضیحِ {@link MessageBroadcaster} نوشته شده.
 * <p>
 * نکتهٔ هم‌روندی: {@code WebSocketSession} برایِ نوشتنِ هم‌زمان از چند نخ امن نیست،
 * پس نوشتن روی هر سشن قفلِ خودِ همان سشن را می‌گیرد. بدونِ این، دو پیامِ هم‌زمان
 * می‌توانند در هم بروند و فریمِ خرابِ WebSocket بسازند.
 */
@Component
public class LocalMessageBroadcaster implements MessageBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(LocalMessageBroadcaster.class);

    private final ObjectMapper objectMapper;

    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    /** کارشناسانِ آنلاین — برایِ تازه‌کردنِ صفِ مشترک، مستقل از اینکه کدام گفت‌وگو باز است. */
    private final Set<WebSocketSession> agentSessions = ConcurrentHashMap.newKeySet();

    public LocalMessageBroadcaster(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void join(String conversationId, WebSocketSession session) {
        rooms.computeIfAbsent(conversationId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void registerAgent(WebSocketSession session) {
        agentSessions.add(session);
    }

    /** با بسته‌شدنِ اتصال باید از همهٔ اتاق‌ها پاک شود، وگرنه رجیستری بی‌نهایت رشد می‌کند. */
    public void remove(WebSocketSession session) {
        agentSessions.remove(session);
        rooms.forEach((conversationId, sessions) -> sessions.remove(session));
        rooms.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    @Override
    public void broadcast(String conversationId, Map<String, Object> payload) {
        send(rooms.getOrDefault(conversationId, Set.of()), payload);
    }

    @Override
    public void broadcastToAgents(Map<String, Object> payload) {
        send(agentSessions, payload);
    }

    private void send(Set<WebSocketSession> targets, Map<String, Object> payload) {
        if (targets.isEmpty()) return;
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (IOException e) {
            log.error("سریالایزِ پاکتِ چت شکست خورد", e);
            return;
        }
        for (WebSocketSession session : targets) {
            if (!session.isOpen()) continue;
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(json));
                }
            } catch (IOException | IllegalStateException e) {
                // اتصالِ یک کاربر نباید پخش به بقیه را متوقف کند؛ منبعِ حقیقت دیتابیس است
                // و همان کاربر بعد از وصلِ دوباره با ?after= جامانده‌ها را می‌گیرد.
                log.debug("ارسال به یک سشنِ چت ناموفق بود: {}", e.getMessage());
            }
        }
    }
}
