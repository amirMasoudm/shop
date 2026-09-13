package org.example.shop1.model.service.analytics;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.shop1.config.AnalyticsProperties;
import org.example.shop1.model.enums.Channel;
import org.example.shop1.model.enums.Device;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

/**
 * برقرارکنندهٔ هویتِ بازدیدکننده — روی <b>همهٔ</b> درخواست‌ها، شاملِ {@code /api/**} و
 * خودِ بیکن.
 * <p>
 * 🔴 <b>چرا از اینترسپتورِ ثبت جداست:</b> اینترسپتور فقط روی صفحه‌های SSR اجرا می‌شود.
 * اگر هویت هم همان‌جا برقرار می‌شد، کاربری که اولین برخوردش با یک مسیرِ داخلیِ SPA یا
 * یک فراخوانیِ API است اصلاً شناسه نمی‌گرفت و کلِ بازدیدش گم می‌شد.
 * <p>
 * عمداً پیش از زنجیرهٔ امنیت اجرا می‌شود تا حتی درخواستی که بعداً ۴۰۱ می‌گیرد هم
 * شناسه داشته باشد. شناسهٔ کاربر اینجا خوانده نمی‌شود؛ {@link UserEventRecorder}
 * آن را موقعِ نوشتنِ رویداد از {@code SecurityContext} می‌گیرد.
 *
 * <h3>دو کوکی</h3>
 * <ul>
 *   <li>{@code dn_aid} — شناسهٔ ناشناس، یک‌ساله. <b>سرور می‌سازدش</b> و
 *       {@code HttpOnly} است، پس جاوااسکریپت نه می‌خواندش نه دستکاری‌اش می‌کند.</li>
 *   <li>{@code dn_sid} — بازدیدِ جاری به‌همراهِ کانال و کارزارش. عمرش برابرِ پنجرهٔ
 *       بی‌حرکتی است و هر درخواست تمدید می‌شود، پس <b>انقضای خودِ کوکی</b> همان قاعدهٔ
 *       «۳۰ دقیقه بی‌حرکتی = بازدیدِ تازه» را بدونِ هیچ حالتی روی سرور پیاده می‌کند.</li>
 * </ul>
 * کانال داخلِ همین کوکی حمل می‌شود تا هر رویداد — از جمله رویدادهای بیکن — بتواند
 * بدونِ هیچ کوئری‌ای {@code channel} را روی خودش بنشاند. این همان چیزی است که فازِ ۲
 * به آن گره خورده.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class VisitorIdentityFilter extends OncePerRequestFilter {

    private static final String ANON_COOKIE = "dn_aid";
    private static final String SESSION_COOKIE = "dn_sid";

    private final AnalyticsProperties props;
    private final ChannelResolver channelResolver;
    private final GeoCityService geo;

    public VisitorIdentityFilter(AnalyticsProperties props, ChannelResolver channelResolver, GeoCityService geo) {
        this.props = props;
        this.channelResolver = channelResolver;
        this.geo = geo;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // استاتیک‌ها شناسه لازم ندارند؛ کوکی روی اولین صفحه یا اولین فراخوانیِ API ست می‌شود.
        return AnalyticsPaths.isStaticAsset(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            establish(request, response);
        } catch (Exception e) {
            // ردگیری هرگز نباید جلوی خودِ سایت را بگیرد.
            logger.debug("برقراریِ هویتِ ردگیری ناموفق بود: " + e);
        }
        chain.doFilter(request, response);
    }

    private void establish(HttpServletRequest request, HttpServletResponse response) {
        String anonId = cookie(request, ANON_COOKIE);
        if (anonId == null || !isUuid(anonId)) {
            anonId = UUID.randomUUID().toString();
            writeCookie(request, response, ANON_COOKIE, anonId, props.getAnonCookieDays() * 24 * 3600);
        }

        String sessionCookie = cookie(request, SESSION_COOKIE);
        String sessionId;
        Channel channel;
        String campaign;
        Channel channelFromCookie = Channel.DIRECT;
        String campaignFromCookie = null;
        boolean newSession = false;

        // ⚠️ کوکی HttpOnly است، ولی این یعنی «جاوااسکریپتِ صفحه نمی‌تواند دستکاری کند»،
        // نه «قابلِ جعل نیست»: هر کلاینتی می‌تواند هدرِ Cookie را دستی بفرستد. پس هر
        // چیزی که از این کوکی بیرون می‌آید ورودیِ بیرونی است و اعتبارسنجی می‌خواهد —
        // دقیقاً همان کاری که برایِ dn_aid انجام می‌شود.
        String cookieSessionId = null;
        if (sessionCookie != null && !sessionCookie.isBlank()) {
            String[] parts = sessionCookie.split("\\.", 3);
            String candidate = decode(parts[0]);
            if (isUuid(candidate)) {
                cookieSessionId = candidate;
                channelFromCookie = parts.length > 1 ? parseChannel(parts[1]) : Channel.DIRECT;
                // سقفِ ۱۲۰ کاراکتریِ ChannelResolver.param فقط روی اولین درخواست اعمال
                // می‌شود؛ مسیرِ خواندن از کوکی دورش می‌زد و کارزارِ بلند روی تک‌تکِ
                // رویدادهای آن بازدید تکرار می‌شد.
                campaignFromCookie = trimCampaign(parts.length > 2 ? decode(parts[2]) : null);
            }
        }

        // 🔴 ورودِ یک «منبعِ تازه» بازدید را از نو شروع می‌کند، حتی اگر سشنِ قبلی زنده
        // باشد. پیش از این، منبع فقط وقتی حساب می‌شد که کوکی نبود — یعنی اگر کسی در
        // نیم‌ساعتِ گذشته سایت را دیده بود و بعد روی لینکِ کارزارِ ما کلیک می‌کرد،
        // دقیقاً همان کلیک نامرئی می‌شد. ارزشمندترین لحظهٔ انتساب را از دست می‌دادیم.
        TrafficSource incoming = channelResolver.resolve(request);
        boolean newSourceArrived = cookieSessionId != null && (
                channelResolver.hasCampaignParams(request)
                        // ارجاع از دامنهٔ خودمان در resolve نال می‌شود، پس اینجا فقط
                        // ارجاعِ واقعاً بیرونی می‌ماند و ناوبریِ داخلی سشن نمی‌شکند.
                        || (incoming.referrerHost() != null && incoming.channel() != channelFromCookie));

        if (cookieSessionId == null || newSourceArrived) {
            // بازدیدِ تازه — کوکی نبود، شناسه‌اش معتبر نبود، یا منبعِ تازه‌ای رسید.
            sessionId = UUID.randomUUID().toString();
            channel = incoming.channel();
            campaign = trimCampaign(incoming.campaign());
            newSession = true;
            request.setAttribute(TrafficSource.class.getName(), incoming);
        } else {
            sessionId = cookieSessionId;
            channel = channelFromCookie;
            campaign = campaignFromCookie;
        }

        // تمدید در هر درخواست — پنجرهٔ بی‌حرکتی از «آخرین فعالیت» شمرده می‌شود، نه از شروعِ بازدید.
        writeCookie(request, response, SESSION_COOKIE,
                encode(sessionId) + "." + channel.name() + "." + encode(campaign),
                props.getSessionMinutes() * 60);

        String truncatedIp = geo.truncatedIp(request);   // ← IPِ کامل هرگز از اینجا بیرون نمی‌رود
        AnalyticsContext ctx = new AnalyticsContext(anonId, sessionId, newSession,
                deviceOf(request.getHeader("User-Agent")), geo.cityOf(truncatedIp), channel, campaign);
        request.setAttribute(AnalyticsContext.REQUEST_ATTRIBUTE, ctx);
    }

    /**
     * نوعِ دستگاه از User-Agent.
     * <p>
     * رشتهٔ خام ذخیره نمی‌شود؛ فقط همین سه مقدارِ مشتق‌شده. تشخیص عمداً ساده است —
     * برایِ «چند درصد موبایل‌اند» دقتِ بیشتری لازم نیست و کتابخانهٔ اضافه ارزشش را ندارد.
     */
    private Device deviceOf(String userAgent) {
        if (userAgent == null) return Device.DESKTOP;
        String ua = userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("ipad") || ua.contains("tablet") || (ua.contains("android") && !ua.contains("mobile"))) {
            return Device.TABLET;
        }
        if (ua.contains("mobi") || ua.contains("iphone") || ua.contains("android")) {
            return Device.MOBILE;
        }
        return Device.DESKTOP;
    }

    /**
     * کوکی با {@code SameSite} — با هدرِ خام نوشته می‌شود چون {@link Cookie} در سرولت
     * صفتِ SameSite ندارد. {@code Secure} فقط وقتی درخواست HTTPS است، وگرنه روی
     * HTTPِ فعلی کوکی اصلاً ست نمی‌شود.
     */
    private void writeCookie(HttpServletRequest request, HttpServletResponse response,
                             String name, String value, int maxAgeSeconds) {
        StringBuilder sb = new StringBuilder()
                .append(name).append('=').append(value)
                .append("; Max-Age=").append(maxAgeSeconds)
                .append("; Path=/; HttpOnly; SameSite=Lax");
        if (request.isSecure()) sb.append("; Secure");
        response.addHeader("Set-Cookie", sb.toString());
    }

    private String cookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private Channel parseChannel(String raw) {
        try {
            return Channel.valueOf(raw);
        } catch (Exception e) {
            return Channel.DIRECT;
        }
    }

    /** همان سقفی که {@code ChannelResolver.param} روی UTMِ درخواستِ اول می‌گذارد. */
    private static final int MAX_CAMPAIGN_LENGTH = 120;

    private String trimCampaign(String campaign) {
        if (campaign == null) return null;
        String c = campaign.trim();
        if (c.isEmpty()) return null;
        return c.length() > MAX_CAMPAIGN_LENGTH ? c.substring(0, MAX_CAMPAIGN_LENGTH) : c;
    }

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String encode(String value) {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String decode(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
