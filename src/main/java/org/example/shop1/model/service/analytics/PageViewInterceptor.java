package org.example.shop1.model.service.analytics;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.shop1.model.enums.EventType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ثبتِ {@code SESSION_START} و {@code PAGE_VIEW} — فقط روی صفحه‌های SSR.
 * <p>
 * جدا از {@link VisitorIdentityFilter} است: آن فیلتر روی همه‌چیز اجرا می‌شود تا هویت
 * برقرار شود، این فقط جایی می‌نویسد که واقعاً «یک صفحه دیده شده». استاتیک‌ها و
 * {@code /api/**} مستثنا هستند، وگرنه هر لودِ صفحه ده‌ها رویدادِ بی‌معنی می‌ساخت.
 * <p>
 * در {@code afterCompletion} می‌نویسد نه {@code preHandle}، تا صفحه‌ای که ۳۰۲/۴۰۴
 * شده «بازدید» حساب نشود.
 */
@Component
public class PageViewInterceptor implements HandlerInterceptor {

    private final UserEventRecorder recorder;

    public PageViewInterceptor(UserEventRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        String uri = request.getRequestURI();
        if (!AnalyticsPaths.isTrackablePage(uri)) return;
        if (response.getStatus() < 200 || response.getStatus() >= 300) return;

        AnalyticsContext ctx = (AnalyticsContext) request.getAttribute(AnalyticsContext.REQUEST_ATTRIBUTE);
        if (ctx == null) return;

        if (ctx.isNewSession()) {
            TrafficSource source = (TrafficSource) request.getAttribute(TrafficSource.class.getName());
            if (source == null) source = TrafficSource.direct(uri);

            // 🔴 بازدیدکننده فقط همین‌جا نوشته می‌شود، نه روی هر مشاهدهٔ صفحه — وگرنه
            // هر لودِ صفحه یک نوشتنِ اضافه به مونگو می‌زد.
            recorder.touchVisitor(ctx, source);
            recorder.record(ctx, EventType.SESSION_START, uri, null, null, null, sourceProps(source), null);
        }

        recorder.record(ctx, EventType.PAGE_VIEW, uri, null, null, null, null, null);
    }

    /** بستهٔ منبعِ ورود فقط روی {@code SESSION_START} می‌نشیند؛ کانالش اما روی هر رویداد است. */
    private Map<String, Object> sourceProps(TrafficSource source) {
        Map<String, Object> props = new LinkedHashMap<>();
        if (source.referrerHost() != null) props.put("referrerHost", source.referrerHost());
        if (source.landingPath() != null) props.put("landingPath", source.landingPath());
        if (source.source() != null) props.put("utmSource", source.source());
        if (source.medium() != null) props.put("utmMedium", source.medium());
        if (source.campaign() != null) props.put("utmCampaign", source.campaign());
        if (source.term() != null) props.put("utmTerm", source.term());
        if (source.content() != null) props.put("utmContent", source.content());
        return props;
    }
}
