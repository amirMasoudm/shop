package org.example.shop1.model.entity;

import org.example.shop1.model.enums.Channel;
import org.example.shop1.model.enums.Device;
import org.example.shop1.model.enums.EventType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * یک رویدادِ رفتاریِ کاربر.
 * <p>
 * 🔴 <b>این کالکشن با {@code activity_logs} قاطی نشود.</b> آن لاگِ ممیزیِ کارکنان است
 * و تضمینِ «حذف‌نشدنی» دارد؛ این دادهٔ آماریِ مشتری است که بعد از ۹۰ روز آرشیو و حذف
 * می‌شود. قاطی‌کردنشان یا تضمینِ ممیزی را می‌شکند یا صفحهٔ لاگ را زیرِ حجم دفن می‌کند.
 * <p>
 * <b>چه چیزی عمداً اینجا نیست:</b> IPِ خام (فقط {@code /24} و فقط برای شهر)،
 * User-Agentِ خام (فقط {@code device}ِ مشتق‌شده)، و متنِ پیامِ چت.
 */
@Document(collection = "user_events")
public class UserEvent {

    @Id
    private String id;

    private Instant at = Instant.now();

    private String anonId;

    /** خالی برای بازدیدکنندهٔ ناشناس. همیشه سمتِ سرور تعیین می‌شود، نه از بدنهٔ درخواست. */
    private String userId;

    private String sessionId;

    private EventType type;

    private String path;

    /** PRODUCT | CATEGORY | ARTICLE | COURSE */
    private String entityType;
    private String entityId;

    /**
     * اسنپ‌شاتِ نام — همان ترفندی که در {@link ActivityLog} جواب داد. بدونِ آن، گزارشِ
     * سه‌ماه‌پیش بعد از حذفِ یک محصول به ردیف‌های بی‌نام تبدیل می‌شود.
     */
    private String entityName;

    /** محدود: حداکثر ۱۰ کلید و ~۵۰۰ بایت (در سرویس اعمال می‌شود). */
    private Map<String, Object> props;

    private Device device;

    /** از IPِ بریده‌شده به {@code /24}. اگر فایلِ ژئو نباشد، خالی می‌ماند. */
    private String city;

    /**
     * 🔴 <b>عمداً روی هر رویداد تکرار می‌شود، نه فقط روی {@code SESSION_START}.</b>
     * <p>
     * منبعِ ورود ذاتاً مالِ بازدید است، ولی اگر فقط آنجا بنشیند، جمع‌بندیِ شبانهٔ فازِ ۲
     * مجبور می‌شود برای هر بازدیدِ آن روز اول کانالش را پیدا کند و بعد رویدادهایش را
     * نسبت دهد — یک اتصالِ سنگین روی ده‌ها هزار بازدید، هر شب، روی سرورِ دو هسته‌ای.
     * با ~۱۵ بایت تکرار، جمع‌بندی یک {@code group by} ساده می‌شود.
     */
    private Channel channel;

    /** به همان دلیلِ {@link #channel} تکرار می‌شود؛ گزارشِ کارزارِ فازِ ۳ رویش بنا می‌شود. */
    private String campaign;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Instant getAt() { return at; }
    public void setAt(Instant at) { this.at = at; }

    public String getAnonId() { return anonId; }
    public void setAnonId(String anonId) { this.anonId = anonId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }

    public Map<String, Object> getProps() { return props; }
    public void setProps(Map<String, Object> props) { this.props = props; }

    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }

    public String getCampaign() { return campaign; }
    public void setCampaign(String campaign) { this.campaign = campaign; }
}
