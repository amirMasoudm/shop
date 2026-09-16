package org.example.shop1.model.entity;

import org.example.shop1.model.enums.Channel;
import org.example.shop1.model.enums.Device;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * یک سند به‌ازای هر {@code anonId} — هم‌زمان دو کار می‌کند: <b>دوختنِ هویت</b> و
 * <b>نگه‌داشتنِ اولین منبعِ ورود</b>.
 * <p>
 * ⚠️ <b>رابطه یک‌به‌چند است، نه یک‌به‌یک:</b> یک کاربر گوشی، لپ‌تاپ و مرورگرِ دیگر
 * دارد، پس چند {@code anonId}. «همهٔ شناسه‌های ناشناسِ این کاربر» یک کوئری روی
 * {@code userId} است.
 * <p>
 * بدونِ این سند، «به تفکیکِ کاربر» عملاً کار نمی‌کند: کاربر روزِ اول ناشناس می‌گردد و
 * روزِ سوم وارد می‌شود، و آن دو تکه هرگز به هم نمی‌چسبند.
 */
@Document(collection = "visitors")
public class Visitor {

    /**
     * اولین منبعِ ورود.
     * <p>
     * 🔴 <b>هرگز بازنویسی نمی‌شود.</b> کسی اولین‌بار از ترب می‌آید و هفتهٔ بعد مستقیم
     * برمی‌گردد و می‌خرد؛ اگر بازنویسی شود، اعتبارِ فروش به «مستقیم» می‌رود و ترب صفر
     * می‌شود — و ممکن است کانالی را ببندیم که در واقع مشتری می‌آورد.
     */
    public static class FirstTouch {
        private Channel channel;
        private String source;
        private String medium;
        private String campaign;
        private String referrerHost;
        private String landingPath;

        public Channel getChannel() { return channel; }
        public void setChannel(Channel channel) { this.channel = channel; }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }

        public String getMedium() { return medium; }
        public void setMedium(String medium) { this.medium = medium; }

        public String getCampaign() { return campaign; }
        public void setCampaign(String campaign) { this.campaign = campaign; }

        public String getReferrerHost() { return referrerHost; }
        public void setReferrerHost(String referrerHost) { this.referrerHost = referrerHost; }

        public String getLandingPath() { return landingPath; }
        public void setLandingPath(String landingPath) { this.landingPath = landingPath; }
    }

    /**
     * فعالیتی که رویدادهای خامش دیگر در دیتابیس نیست، چون آرشیو شده.
     * <p>
     * 🔴 <b>چرا لازم است:</b> ستون‌های «بازدید»، «مشاهدهٔ محصول» و «سفارش» در نمای
     * بازدیدکنندگان زندهٔ زنده از {@code user_events} شمرده می‌شوند. اولین آرشیو که
     * اجرا شد، آن رویدادها از دیتابیس رفتند و هر سه ستون برای همه صفر شد — انگار
     * این آدم هیچ‌وقت نیامده. حالا چرخهٔ آرشیو قبل از حذف، شمارشِ هر شناسه را
     * همین‌جا جمع می‌زند و نما جمعِ زنده و آرشیو را نشان می‌دهد.
     * <p>
     * ⚠️ فقط انباشته می‌شود و هیچ‌وقت از نو حساب نمی‌شود؛ رویدادهای خامش رفته‌اند و
     * تنها منبعِ بازسازی‌اش فایلِ آرشیو است. پس آرشیوِ دوباره روی همان بازه —
     * که پیش نمی‌آید چون رویدادها حذف شده‌اند — دوبار می‌شمرد.
     */
    public static class Archived {
        /** تعدادِ سشن‌های متمایز، نه تعدادِ رویداد. */
        private long visits;
        private long productViews;
        private long orders;
        /** بازهٔ رویدادهای آرشیوشدهٔ همین شناسه — در پنل شمسی نمایش داده می‌شود. */
        private Instant from;
        private Instant to;

        public long getVisits() { return visits; }
        public void setVisits(long visits) { this.visits = visits; }

        public long getProductViews() { return productViews; }
        public void setProductViews(long productViews) { this.productViews = productViews; }

        public long getOrders() { return orders; }
        public void setOrders(long orders) { this.orders = orders; }

        public Instant getFrom() { return from; }
        public void setFrom(Instant from) { this.from = from; }

        public Instant getTo() { return to; }
        public void setTo(Instant to) { this.to = to; }
    }

    /** خودِ {@code anonId} است. */
    @Id
    private String id;

    /** بعد از ورود/ثبت‌نامِ موفق پر می‌شود. ایندکس دارد. */
    private String userId;

    private Instant firstSeenAt = Instant.now();

    /**
     * 🔴 فقط روی {@code SESSION_START} به‌روز می‌شود، نه روی هر مشاهدهٔ صفحه — وگرنه هر
     * لودِ صفحه یک نوشتنِ اضافه به دیتابیس می‌زند و کلِ فایدهٔ صفِ دسته‌ای از بین می‌رود.
     * «آخرین حضور» در دقتِ یک بازدید کاملاً کافی است.
     */
    private Instant lastSeenAt = Instant.now();

    private FirstTouch firstTouch;

    private String lastCity;
    private Device lastDevice;

    /**
     * منبعِ <b>آخرین</b> بازدید — در کنارِ {@link #firstTouch}، نه به‌جایش.
     * <p>
     * 🔴 چرا لازم شد: {@code firstTouch} عمداً هرگز بازنویسی نمی‌شود، و همین درست
     * است. ولی نتیجه‌اش این بود که وقتی مالک لینکِ کارزار را در مرورگری باز کرد که
     * قبلاً به سایت آمده بود، ردیفِ آن بازدیدکننده در نمای «کاربران» همچنان «مستقیم»
     * نشان می‌داد — رویدادها درست ثبت شده بودند ولی در آن جدول پیدا نبودند و به‌نظر
     * می‌رسید کارزار اصلاً لاگ نمی‌شود.
     * <p>
     * پس اولین منبع برای انتساب می‌ماند، و این یکی می‌گوید «این بار از کجا آمد».
     */
    private Channel lastChannel;
    private String lastCampaign;

    /** {@code null} تا وقتی هیچ رویدادی از این شناسه آرشیو نشده باشد. */
    private Archived archived;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Instant getFirstSeenAt() { return firstSeenAt; }
    public void setFirstSeenAt(Instant firstSeenAt) { this.firstSeenAt = firstSeenAt; }

    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public FirstTouch getFirstTouch() { return firstTouch; }
    public void setFirstTouch(FirstTouch firstTouch) { this.firstTouch = firstTouch; }

    public String getLastCity() { return lastCity; }
    public void setLastCity(String lastCity) { this.lastCity = lastCity; }

    public Device getLastDevice() { return lastDevice; }
    public void setLastDevice(Device lastDevice) { this.lastDevice = lastDevice; }

    public Channel getLastChannel() { return lastChannel; }
    public void setLastChannel(Channel lastChannel) { this.lastChannel = lastChannel; }

    public String getLastCampaign() { return lastCampaign; }
    public void setLastCampaign(String lastCampaign) { this.lastCampaign = lastCampaign; }

    public Archived getArchived() { return archived; }
    public void setArchived(Archived archived) { this.archived = archived; }
}
