package org.example.shop1.model.service.analytics;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.shop1.config.AnalyticsProperties;
import org.example.shop1.model.enums.Channel;
import org.example.shop1.model.enums.Device;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * نویسندهٔ کوکی‌های هویت.
 * <p>
 * از {@link VisitorIdentityFilter} جدا شد چون یک مصرف‌کنندهٔ دوم پیدا کرد: لینکِ
 * کوتاهِ کارزار باید <b>پیش از ریدایرکت</b> بازدید را با کانال و کارزارِ درست شروع
 * کند. اگر منتظرِ درخواستِ بعدی بمانیم، کلیکی که صفحهٔ مقصدش هرگز باز نمی‌شود —
 * بستنِ زودهنگام، اینترنتِ کند، ربات — اصلاً ثبت نمی‌شود.
 * <p>
 * کنترلری که به فیلتر تزریق شود بوی بدی می‌دهد؛ این کلاسِ کوچک همان کارِ مشترک را
 * بدونِ آن وابستگی می‌دهد.
 */
@Component
public class VisitorSessionWriter {

    public static final String ANON_COOKIE = "dn_aid";
    public static final String SESSION_COOKIE = "dn_sid";

    private final AnalyticsProperties props;

    public VisitorSessionWriter(AnalyticsProperties props) {
        this.props = props;
    }

    public void writeAnonCookie(HttpServletRequest request, HttpServletResponse response, String anonId) {
        write(request, response, ANON_COOKIE, anonId, props.getAnonCookieDays() * 24 * 3600);
    }

    public void writeSessionCookie(HttpServletRequest request, HttpServletResponse response,
                                   String sessionId, Channel channel, String campaign) {
        write(request, response, SESSION_COOKIE,
                encode(sessionId) + "." + channel.name() + "." + encode(campaign),
                props.getSessionMinutes() * 60);
    }

    /**
     * شروعِ بازدیدِ تازه با کانال و کارزارِ صریح، وسطِ همین درخواست.
     * <p>
     * زمینهٔ ردگیریِ درخواست هم جایگزین می‌شود تا رویدادهایی که <b>در همین درخواست</b>
     * ثبت می‌شوند — {@code LINK_CLICK} و {@code SESSION_START} — شناسهٔ بازدیدِ تازه
     * را بگیرند، نه بازدیدِ قبلی را.
     */
    public AnalyticsContext startSession(HttpServletRequest request, HttpServletResponse response,
                                         Channel channel, String campaign) {
        AnalyticsContext current = (AnalyticsContext) request.getAttribute(AnalyticsContext.REQUEST_ATTRIBUTE);
        String anonId = current != null ? current.anonId() : UUID.randomUUID().toString();
        if (current == null) writeAnonCookie(request, response, anonId);

        String sessionId = UUID.randomUUID().toString();
        writeSessionCookie(request, response, sessionId, channel, campaign);

        AnalyticsContext next = new AnalyticsContext(anonId, sessionId, true,
                current != null ? current.device() : Device.DESKTOP,
                current != null ? current.city() : null,
                channel, campaign);
        request.setAttribute(AnalyticsContext.REQUEST_ATTRIBUTE, next);
        return next;
    }

    /**
     * کوکی با {@code SameSite} — با هدرِ خام نوشته می‌شود چون {@code Cookie} در سرولت
     * صفتِ SameSite ندارد. {@code Secure} فقط وقتی درخواست HTTPS است، وگرنه روی
     * HTTPِ فعلی کوکی اصلاً ست نمی‌شود.
     */
    private void write(HttpServletRequest request, HttpServletResponse response,
                       String name, String value, int maxAgeSeconds) {
        StringBuilder sb = new StringBuilder()
                .append(name).append('=').append(value)
                .append("; Max-Age=").append(maxAgeSeconds)
                .append("; Path=/; HttpOnly; SameSite=Lax");
        if (request.isSecure()) sb.append("; Secure");
        response.addHeader("Set-Cookie", sb.toString());
    }

    private static String encode(String value) {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
