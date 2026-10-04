package org.example.shop1.model.service.analytics;

import org.example.shop1.model.enums.Channel;
import org.example.shop1.model.enums.Device;

/**
 * هویت و زمینهٔ ردگیریِ درخواستِ جاری — فیلترِ هویت می‌سازدش و روی
 * {@code HttpServletRequest} می‌گذارد تا اینترسپتور، کنترلرِ بیکن و قلاب‌های داخلِ
 * سرویس‌ها همه از یک منبع بخوانند.
 * <p>
 * {@code userId} عمداً اینجا نیست: در لحظهٔ ساختِ این شیء ممکن است هنوز احراز هویت
 * انجام نشده باشد (مثلاً خودِ درخواستِ ورود). ثبت‌کننده آن را موقعِ نوشتنِ رویداد از
 * {@code SecurityContext} می‌خواند، نه از بدنهٔ درخواست.
 */
public class AnalyticsContext {

    public static final String REQUEST_ATTRIBUTE = "dn.analytics.ctx";

    private final String anonId;
    private final String sessionId;
    private final boolean newSession;
    private final Device device;
    private final String city;
    private final Channel channel;
    private final String campaign;

    public AnalyticsContext(String anonId, String sessionId, boolean newSession,
                            Device device, String city, Channel channel, String campaign) {
        this.anonId = anonId;
        this.sessionId = sessionId;
        this.newSession = newSession;
        this.device = device;
        this.city = city;
        this.channel = channel;
        this.campaign = campaign;
    }

    public String anonId() { return anonId; }
    public String sessionId() { return sessionId; }

    /** آیا این درخواست شروعِ یک بازدیدِ تازه است — اینترسپتور با آن {@code SESSION_START} می‌زند. */
    public boolean isNewSession() { return newSession; }

    public Device device() { return device; }
    public String city() { return city; }
    public Channel channel() { return channel; }
    public String campaign() { return campaign; }
}
