package org.example.shop1.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * ثبتِ اندپوینتِ وب‌سوکتِ چت.
 * <p>
 * ⚠️ {@code /ws/chat} پشتِ nginx نیاز به یک {@code location} جدا با هدرهای
 * {@code Upgrade}/{@code Connection} دارد. بلاکِ عمومیِ {@code location /} در
 * {@code nginx/nginx.conf} عمداً {@code Connection ""} می‌گذارد (لازمهٔ keepalive)
 * و همان مقدار ارتقا به وب‌سوکت را می‌شکند.
 * <p>
 * {@code setAllowedOrigins} تنظیم نشده تا پیش‌فرضِ اسپرینگ (فقط same-origin) اعمال
 * شود؛ چت فقط از خودِ فروشگاه باز می‌شود.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ChatHandshakeInterceptor handshakeInterceptor;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler,
                           ChatHandshakeInterceptor handshakeInterceptor) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(handshakeInterceptor);
    }
}
