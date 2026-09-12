package org.example.shop1.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * تنظیماتِ ردگیریِ رفتارِ کاربر.
 * <p>
 * ⚠️ جدولِ دامنه‌ها عمداً <b>اینجا مقدارِ پیش‌فرضِ جاوایی ندارد</b> و از
 * {@code application.properties} می‌آید: هم خواستهٔ تسک است («قابلِ تنظیم، نه هاردکدِ
 * داخلِ متد»)، هم قاعدهٔ جداسازیِ اپِ ژنریک — «ترب» و «ایمالز» دانشِ کسب‌وکارِ یک
 * فروشگاهِ مشخص‌اند، نه بخشی از هستهٔ اپ.
 * <p>
 * همهٔ کلیدها در {@code application.properties} مقدار دارند، پس <b>هیچ متغیرِ محیطیِ
 * تازه‌ای لازم نیست</b> و کامپوزِ سرور دست‌نخورده می‌ماند.
 */
@Component
@ConfigurationProperties(prefix = "app.analytics")
public class AnalyticsProperties {

    /** مدتِ نگه‌داریِ دادهٔ خام. جابِ آرشیو-سپس-حذف از همین می‌خواند. */
    private int retentionDays = 90;

    /**
     * منطقهٔ زمانی‌ای که مرزِ «یک روز» را در جمع‌بندی تعیین می‌کند. خالی = زمانِ سرور.
     * <p>
     * بدونِ این، مرزِ روز به منطقهٔ زمانیِ ماشین گره می‌خورد و جابه‌جاییِ سرور
     * عددهای تاریخیِ جمع‌بندی را بی‌صدا جابه‌جا می‌کرد.
     */
    private String zone = "";

    /** پوشهٔ آرشیو — بیرونِ هر مسیری که سرو می‌شود. */
    private String archiveDir = "/opt/shop/analytics-archive";

    /** از این حجم که گذشت هشدار داده می‌شود؛ هیچ آرشیوی خودکار حذف نمی‌شود. */
    private long archiveWarnMb = 2048;

    /** کرانِ بالای صفِ درون‌حافظه‌ای. پر که شد، رویداد دور ریخته می‌شود. */
    private int queueCapacity = 10_000;

    /** بیشترین تعدادِ رویداد در هر {@code insertMany}. */
    private int flushBatchSize = 500;

    /** بی‌حرکتیِ بیشتر از این، یعنی بازدیدِ بعدی «بازدیدِ تازه» است. */
    private int sessionMinutes = 30;

    /** عمرِ کوکیِ {@code dn_aid} به روز. */
    private int anonCookieDays = 365;

    private List<String> searchHosts = List.of();
    private List<String> socialHosts = List.of();
    private List<String> marketplaceHosts = List.of();

    /** مقادیرِ {@code utm_medium} که یعنی ترافیکِ پولی. */
    private List<String> paidMediums = List.of();

    /**
     * مسیرِ فایلِ CSVِ ژئو (IPِ شروع، IPِ پایان، …، شهر). خالی = شهر ثبت نمی‌شود.
     * <p>
     * 🔴 هیچ فراخوانیِ APIِ بیرونی در مسیرِ درخواست نیست؛ فایل یک‌بار هنگامِ بالاآمدن
     * خوانده می‌شود. از سرورِ ایران، وابستگیِ بیرونی قابلِ اتکا نیست.
     */
    private String geoCsv = "";

    /** سقفِ {@code props} — بدونِ سقف، اولین باتی که پیدایش کند دیتابیس را پر می‌کند. */
    private int maxPropKeys = 10;
    private int maxPropBytes = 500;

    /** سقفِ هر بستهٔ بیکن. */
    private int beaconMaxEvents = 20;
    private int beaconMaxBytes = 8192;

    /** سقفِ رویداد در دقیقه، هم روی IP و هم روی {@code anonId}. */
    private int rateLimitPerMinute = 120;

    public int getRetentionDays() { return retentionDays; }
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public String getArchiveDir() { return archiveDir; }
    public void setArchiveDir(String archiveDir) { this.archiveDir = archiveDir; }

    public long getArchiveWarnMb() { return archiveWarnMb; }
    public void setArchiveWarnMb(long archiveWarnMb) { this.archiveWarnMb = archiveWarnMb; }

    public int getQueueCapacity() { return queueCapacity; }
    public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }

    public int getFlushBatchSize() { return flushBatchSize; }
    public void setFlushBatchSize(int flushBatchSize) { this.flushBatchSize = flushBatchSize; }

    public int getSessionMinutes() { return sessionMinutes; }
    public void setSessionMinutes(int sessionMinutes) { this.sessionMinutes = sessionMinutes; }

    public int getAnonCookieDays() { return anonCookieDays; }
    public void setAnonCookieDays(int anonCookieDays) { this.anonCookieDays = anonCookieDays; }

    public List<String> getSearchHosts() { return searchHosts; }
    public void setSearchHosts(List<String> searchHosts) { this.searchHosts = searchHosts; }

    public List<String> getSocialHosts() { return socialHosts; }
    public void setSocialHosts(List<String> socialHosts) { this.socialHosts = socialHosts; }

    public List<String> getMarketplaceHosts() { return marketplaceHosts; }
    public void setMarketplaceHosts(List<String> marketplaceHosts) { this.marketplaceHosts = marketplaceHosts; }

    public List<String> getPaidMediums() { return paidMediums; }
    public void setPaidMediums(List<String> paidMediums) { this.paidMediums = paidMediums; }

    public String getGeoCsv() { return geoCsv; }
    public void setGeoCsv(String geoCsv) { this.geoCsv = geoCsv; }

    public int getMaxPropKeys() { return maxPropKeys; }
    public void setMaxPropKeys(int maxPropKeys) { this.maxPropKeys = maxPropKeys; }

    public int getMaxPropBytes() { return maxPropBytes; }
    public void setMaxPropBytes(int maxPropBytes) { this.maxPropBytes = maxPropBytes; }

    public int getBeaconMaxEvents() { return beaconMaxEvents; }
    public void setBeaconMaxEvents(int beaconMaxEvents) { this.beaconMaxEvents = beaconMaxEvents; }

    public int getBeaconMaxBytes() { return beaconMaxBytes; }
    public void setBeaconMaxBytes(int beaconMaxBytes) { this.beaconMaxBytes = beaconMaxBytes; }

    public int getRateLimitPerMinute() { return rateLimitPerMinute; }
    public void setRateLimitPerMinute(int rateLimitPerMinute) { this.rateLimitPerMinute = rateLimitPerMinute; }
}
