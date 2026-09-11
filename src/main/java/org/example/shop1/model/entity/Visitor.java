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
}
