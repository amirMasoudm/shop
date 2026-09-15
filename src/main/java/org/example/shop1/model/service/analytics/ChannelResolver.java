package org.example.shop1.model.service.analytics;

import jakarta.servlet.http.HttpServletRequest;
import org.example.shop1.config.AnalyticsProperties;
import org.example.shop1.model.enums.Channel;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Locale;

/**
 * تشخیصِ «از کجا و از چه راهی آمده» — سمتِ سرور، نه با جاوااسکریپت.
 * <p>
 * چرا سروری: هدرِ {@code Referer} و کوئریِ UTM روی <b>همان اولین درخواست</b> هستند.
 * اگر این کار به جاوااسکریپت سپرده شود، ترافیکی که اسکریپتش اجرا نمی‌شود (افزونهٔ
 * مسدودکننده، قطعیِ شبکه، خزندهٔ ناقص) بی‌منبع می‌ماند.
 * <p>
 * جدولِ دامنه‌ها از {@link AnalyticsProperties} می‌آید، نه هاردکد داخلِ همین متد.
 */
@Service
public class ChannelResolver {

    private final AnalyticsProperties props;

    public ChannelResolver(AnalyticsProperties props) {
        this.props = props;
    }

    /**
     * آیا این درخواست برچسبِ صریحِ کارزار دارد؟
     * <p>
     * وجودِ هر کدام از این‌ها یعنی «منبعِ تازه» و باید بازدید را از نو شروع کند، حتی
     * وقتی سشنِ قبلی زنده است — وگرنه کلیک روی لینکِ کارزار برای کسی که همین حالا در
     * سایت بوده، بی‌انتساب می‌ماند.
     */
    public boolean hasCampaignParams(HttpServletRequest request) {
        return param(request, "utm_source") != null
                || param(request, "utm_medium") != null
                || param(request, "utm_campaign") != null
                || param(request, "utm_term") != null
                || param(request, "utm_content") != null
                || param(request, "gclid") != null;
    }

    /**
     * آیا این درخواست از لینکی در دامنهٔ خودمان آمده؟
     * <p>
     * 🔴 <b>گاردِ «UTM روی لینکِ داخلی»:</b> اگر لینکی از صفحه‌ای در سایتِ خودمان به
     * صفحهٔ دیگری در سایتِ خودمان UTM داشته باشد، نباید منبعِ واقعیِ بازدید را با
     * «خودمان» جایگزین کند. این کلاسیک‌ترین راهِ نابودکردنِ انتساب است: یک بنرِ داخلیِ
     * برچسب‌خورده کافی است تا همهٔ فروش‌ها به‌جای گوگل و ترب به خودمان نسبت داده شود.
     */
    public boolean isInternalReferrer(HttpServletRequest request) {
        String host = hostOf(request.getHeader("Referer"));
        return host != null && isOwnHost(host, request);
    }

    public TrafficSource resolve(HttpServletRequest request) {
        String landingPath = request.getRequestURI();
        String referrerHost = hostOf(request.getHeader("Referer"));

        // ارجاع از دامنهٔ خودمان منبعِ تازه نیست — ناوبریِ داخلی است. اگر این را
        // منبع حساب کنیم، بازدیدِ جاری «ارجاع از خودمان» می‌شود و منبعِ واقعی گم.
        if (referrerHost != null && isOwnHost(referrerHost, request)) {
            referrerHost = null;
        }

        String source = param(request, "utm_source");
        String medium = param(request, "utm_medium");
        String campaign = param(request, "utm_campaign");
        String term = param(request, "utm_term");
        String content = param(request, "utm_content");
        boolean hasGclid = param(request, "gclid") != null;

        Channel channel = classify(referrerHost, source, medium, hasGclid);
        return new TrafficSource(channel, source, medium, campaign, term, content, referrerHost, landingPath);
    }

    /**
     * کانالِ یک کارزارِ ثبت‌شده — فقط از برچسبِ خودش.
     * <p>
     * لینکِ کوتاه را خودمان ساخته‌ایم و برچسبش را خودمان گذاشته‌ایم، پس قابلِ
     * اتکاترین منبعی است که داریم؛ هدرِ {@code Referer} ممکن است اصلاً نرسد و در
     * پیام‌رسان‌ها معمولاً هم نمی‌رسد.
     */
    public Channel classifyCampaign(String source, String medium) {
        return classify(null, source, medium, false);
    }

    private Channel classify(String referrerHost, String source, String medium, boolean hasGclid) {
        if (hasGclid || contains(props.getPaidMediums(), medium)) return Channel.PAID;

        // UTMِ صریح بر حدس از روی ارجاع‌دهنده مقدم است: لینکی که خودمان برچسب زده‌ایم
        // منبعِ قابلِ اتکاتری از هدرِ Referer است (که ممکن است اصلاً نرسد).
        Channel byUtm = classifySourceName(source);
        if (byUtm != null) return byUtm;

        if (referrerHost == null || referrerHost.isBlank()) {
            // بدونِ ارجاع و بدونِ UTM. اگر UTM هست ولی ناشناخته، «ارجاع» صادق‌تر از «مستقیم» است.
            return (source == null || source.isBlank()) ? Channel.DIRECT : Channel.REFERRAL;
        }

        Channel byHost = classifyHost(referrerHost);
        return byHost != null ? byHost : Channel.REFERRAL;
    }

    /** دامنهٔ ارجاع‌دهنده — تطبیقِ سخت‌گیرانه، چون مقدارش از بیرون می‌آید. */
    private Channel classifyHost(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.toLowerCase(Locale.ROOT);
        if (matches(props.getMarketplaceHosts(), v)) return Channel.MARKETPLACE;
        if (matches(props.getSearchHosts(), v)) return Channel.ORGANIC_SEARCH;
        if (matches(props.getSocialHosts(), v)) return Channel.SOCIAL;
        return null;
    }

    /**
     * {@code utm_source} — اینجا مقدار معمولاً <b>نام</b> است نه دامنه:
     * {@code utm_source=telegram}، نه {@code utm_source=t.me}.
     * <p>
     * پس علاوه بر تطبیقِ دامنه، با «برچسبِ اصلیِ» همان دامنه‌های تنظیم‌شده هم مقایسه
     * می‌شود ({@code telegram.org → telegram}، {@code torob.com → torob}). این باعث
     * می‌شود جدولِ دامنه‌ها یک‌جا بماند و مجبور نشویم فهرستِ دومی از «نام»ها نگه داریم
     * که دیر یا زود با اولی واگرا می‌شد.
     */
    private Channel classifySourceName(String value) {
        Channel byHost = classifyHost(value);
        if (byHost != null) return byHost;
        if (value == null || value.isBlank()) return null;
        String v = value.toLowerCase(Locale.ROOT);
        if (matchesLabel(props.getMarketplaceHosts(), v)) return Channel.MARKETPLACE;
        if (matchesLabel(props.getSearchHosts(), v)) return Channel.ORGANIC_SEARCH;
        if (matchesLabel(props.getSocialHosts(), v)) return Channel.SOCIAL;
        return null;
    }

    /** برچسبِ اولِ دامنه؛ برچسب‌های خیلی کوتاه ({@code t.me}) عمداً نادیده گرفته می‌شوند. */
    private boolean matchesLabel(List<String> hosts, String value) {
        for (String h : hosts) {
            String host = h.trim().toLowerCase(Locale.ROOT);
            int dot = host.indexOf('.');
            if (dot < 3) continue;
            if (value.equals(host.substring(0, dot))) return true;
        }
        return false;
    }

    /**
     * تطبیقِ دامنه: یا دقیقاً برابر، یا زیردامنه ({@code www.google.com}).
     * عمداً «شاملِ رشته» نیست — وگرنه {@code notgoogle.com.evil.ir} هم گوگل حساب می‌شد.
     */
    private boolean matches(List<String> hosts, String value) {
        for (String h : hosts) {
            String host = h.trim().toLowerCase(Locale.ROOT);
            if (host.isEmpty()) continue;
            if (value.equals(host) || value.endsWith("." + host)) return true;
        }
        return false;
    }

    private boolean contains(List<String> values, String value) {
        if (value == null) return false;
        String v = value.trim().toLowerCase(Locale.ROOT);
        return values.stream().anyMatch(x -> x.trim().equalsIgnoreCase(v));
    }

    private boolean isOwnHost(String referrerHost, HttpServletRequest request) {
        String serverName = request.getServerName();
        if (serverName == null) return false;
        String own = serverName.toLowerCase(Locale.ROOT);
        return referrerHost.equals(own) || referrerHost.endsWith("." + own) || own.endsWith("." + referrerHost);
    }

    /** فقط دامنه نگه داشته می‌شود، نه آدرسِ کامل — مسیرِ ارجاع‌دهنده دادهٔ اضافه است. */
    private String hostOf(String referer) {
        if (referer == null || referer.isBlank()) return null;
        try {
            String host = URI.create(referer.trim()).getHost();
            if (host == null) return null;
            host = host.toLowerCase(Locale.ROOT);
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
            return null;
        }
    }

    private String param(HttpServletRequest request, String name) {
        String v = request.getParameter(name);
        if (v == null) return null;
        v = v.trim();
        if (v.isEmpty()) return null;
        // نرمال‌سازیِ ورودی: telegram و Telegram نباید در گزارش دو کانال شوند.
        // سقفِ طول هم هست چون UTM را هرکسی با ساختنِ یک لینک می‌تواند پر کند.
        // فاصله‌های داخلی هم یکدست می‌شوند: «نوروز ۱۴۰۵» و «نوروز  ۱۴۰۵» نباید دو
        // کارزار شوند. همان نرمال‌سازی‌ای که موقعِ ساختِ کارزار انجام می‌شود.
        v = v.toLowerCase(Locale.ROOT).replaceAll("\s+", "-");
        return v.length() > 120 ? v.substring(0, 120) : v;
    }
}
