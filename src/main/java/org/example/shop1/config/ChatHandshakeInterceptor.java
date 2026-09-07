package org.example.shop1.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * احرازِ هویتِ هندشیکِ وب‌سوکت از رویِ همان سشنِ اسپرینگ.
 * <p>
 * ⚠️ عمداً به فیلترِ HTTP اکتفا نمی‌کنیم و اینجا صریح چک می‌شود. دلیلش این است که
 * وب‌سوکت بعد از هندشیک یک اتصالِ بلندمدتِ بدونِ فیلترِ درخواست است؛ اگر همین‌جا
 * رد نشود، هر تغییرِ بعدی در قواعدِ مسیرِ SecurityConfig می‌تواند بی‌صدا یک اتصالِ
 * ناشناس را باز بگذارد.
 * <p>
 * سشن با {@code getSession(false)} خوانده می‌شود تا هندشیکِ ناشناس سشنِ تازه نسازد.
 */
@Component
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    /** همان کلیدی که {@code HttpSessionSecurityContextRepository} استفاده می‌کند. */
    private static final String SECURITY_CONTEXT_KEY = "SPRING_SECURITY_CONTEXT";

    public static final String ATTR_USERNAME = "chatUsername";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) return false;

        HttpSession session = servletRequest.getServletRequest().getSession(false);
        if (session == null) return false;

        Object raw = session.getAttribute(SECURITY_CONTEXT_KEY);
        if (!(raw instanceof SecurityContext context)) return false;

        Authentication auth = context.getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
            return false;
        }

        attributes.put(ATTR_USERNAME, auth.getName());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // کاری لازم نیست
    }
}
