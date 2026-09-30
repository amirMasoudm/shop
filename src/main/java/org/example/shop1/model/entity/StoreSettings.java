package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

@Document(collection = "store_settings")
public class StoreSettings {
    @Id
    private String id = "origin_location"; // همیشه ثابت
    private String state;
    private String city;
    private String address;
    private Double lat;
    private Double lng;

    // آستانه‌ی مبلغی فعال‌شدن استعلام پیش‌فاکتور (RFQ). null یا ۰ یعنی غیرفعال.
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal rfqThreshold;

    /**
     * ضریبِ قیمتِ سایت: {@code onlinePrice = partnerBulkPrice × sitePriceFactor}.
     * <p>
     * پیش‌فرض ۱.۰۸ = ۵٪ سودِ فروشنده + ۳٪ سودِ کارگزار.
     * عمداً اینجاست و در کد هاردکد نشده — قبلاً فقط داخلِ اسکریپتِ یک‌بارمصرفِ ایمپورت بود
     * و اگر در اپ هم تکرار می‌شد، دو منبعِ حقیقت می‌شد و دیر یا زود واگرا می‌شدند.
     * یک قراردادِ تجاری است، پس فقط ADMIN تغییرش می‌دهد نه PRICER.
     */
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal sitePriceFactor;

    /** ضریبِ پیش‌فرض وقتی هنوز در تنظیمات ست نشده است. */
    public static final BigDecimal DEFAULT_SITE_PRICE_FACTOR = new BigDecimal("1.08");

    /**
     * ضریبِ «همکار تک» نسبت به «فروش تعدادی»:
     * {@code partnerUnitPrice = partnerBulkPrice × partnerUnitFactor}.
     * <p>
     * خواهرِ {@code sitePriceFactor} و به همان دلیل اینجاست نه در کد: یک قراردادِ
     * تجاری است، پس فقط ADMIN تغییرش می‌دهد و دو منبعِ حقیقت نمی‌سازیم.
     */
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal partnerUnitFactor;

    /** پیش‌فرض: ۵٪ بیشتر از فروش تعدادی. */
    public static final BigDecimal DEFAULT_PARTNER_UNIT_FACTOR = new BigDecimal("1.05");

    public BigDecimal getPartnerUnitFactor() { return partnerUnitFactor; }
    public void setPartnerUnitFactor(BigDecimal partnerUnitFactor) { this.partnerUnitFactor = partnerUnitFactor; }

    public BigDecimal effectivePartnerUnitFactor() {
        return (partnerUnitFactor != null && partnerUnitFactor.compareTo(BigDecimal.ZERO) > 0)
                ? partnerUnitFactor : DEFAULT_PARTNER_UNIT_FACTOR;
    }

    // ===============================
    // ساعتِ کاریِ چتِ پشتیبانی
    // ===============================
    // ⚠️ عمداً هیچ‌کدام مقدارِ پیش‌فرضِ هاردکد ندارند و هیچ فرضِ کسب‌وکاریِ مشخصی
    //    (روزِ کاریِ ایران، منطقهٔ زمانیِ تهران) در کدِ core ننشسته است. اگر تنظیم
    //    نشده باشند، چت «همیشه باز» است — خنثی‌ترین رفتارِ ممکن برایِ اپِ ژنریک.
    //    تنظیمِ واقعی کارِ ادمینِ همان فروشگاه است.

    /** روزهای کاری با شمارهٔ ISO-8601: ۱=دوشنبه … ۷=یکشنبه. خالی/نال = همیشه باز. */
    private java.util.List<Integer> chatWorkingDays;

    /** ساعتِ شروع، قالبِ {@code HH:mm}. نال = همیشه باز. */
    private String chatStartTime;

    /** ساعتِ پایان، قالبِ {@code HH:mm}. نال = همیشه باز. */
    private String chatEndTime;

    /**
     * شناسهٔ منطقهٔ زمانی (مثلاً {@code Asia/Tehran}). نال = منطقهٔ زمانیِ خودِ سرور.
     * <p>
     * وجودِ این فیلد اختیاری نیست: ساعتِ دستگاهِ کاربر قابلِ اعتماد نیست و بررسی سمتِ
     * سرور انجام می‌شود، پس سرور باید بداند «۹ صبح» یعنی ۹ صبحِ کجا.
     */
    private String chatTimeZone;

    public java.util.List<Integer> getChatWorkingDays() { return chatWorkingDays; }
    public void setChatWorkingDays(java.util.List<Integer> chatWorkingDays) { this.chatWorkingDays = chatWorkingDays; }

    public String getChatStartTime() { return chatStartTime; }
    public void setChatStartTime(String chatStartTime) { this.chatStartTime = chatStartTime; }

    public String getChatEndTime() { return chatEndTime; }
    public void setChatEndTime(String chatEndTime) { this.chatEndTime = chatEndTime; }

    public String getChatTimeZone() { return chatTimeZone; }
    public void setChatTimeZone(String chatTimeZone) { this.chatTimeZone = chatTimeZone; }

    /** آیا ساعتِ کاری اصلاً تنظیم شده؟ اگر نه، قفلِ خارج از ساعت اعمال نمی‌شود. */
    public boolean hasChatSchedule() {
        return chatWorkingDays != null && !chatWorkingDays.isEmpty()
                && chatStartTime != null && !chatStartTime.isBlank()
                && chatEndTime != null && !chatEndTime.isBlank();
    }

    // فاصله‌ی چرخشِ خودکارِ اسلایدرِ بنرِ خانه (ثانیه). null یا نامعتبر یعنی پیش‌فرض.
    private Integer bannerRotationSeconds;

    /** پیش‌فرضِ فاصله‌ی چرخشِ بنر وقتی هنوز در تنظیمات ست نشده. */
    public static final int DEFAULT_BANNER_ROTATION_SECONDS = 5;

    public Integer getBannerRotationSeconds() { return bannerRotationSeconds; }
    public void setBannerRotationSeconds(Integer bannerRotationSeconds) { this.bannerRotationSeconds = bannerRotationSeconds; }

    /** فاصله‌ی مؤثر — اگر ست نشده یا نامعتبر بود، پیش‌فرض. */
    public int effectiveBannerRotationSeconds() {
        return (bannerRotationSeconds != null && bannerRotationSeconds > 0)
                ? bannerRotationSeconds : DEFAULT_BANNER_ROTATION_SECONDS;
    }

    // ===== واژگانِ بستهٔ کارزار =====
    // 🔴 اینجا می‌نشینند و نه در کد، چون دانشِ کسب‌وکارِ یک فروشگاهِ مشخص‌اند و هر
    //    نصبِ دیگری فهرستِ خودش را دارد. ادمین می‌تواند آگاهانه اضافه کند، ولی
    //    کاربرِ لینک‌ساز فقط از همین فهرست انتخاب می‌کند — نه متنِ آزاد.
    //
    // چرا بسته: با ورودیِ آزاد، telegram و Telegram و tg در گزارش سه کانالِ متفاوت
    // می‌شوند. این شایع‌ترین شکستِ برچسب‌گذاری است و با انضباطِ فردی حل نمی‌شود.

    /** اگر تنظیمات هنوز پر نشده، همین‌ها مبنا هستند. */
    public static final java.util.List<String> DEFAULT_CAMPAIGN_SOURCES = java.util.List.of(
            "telegram", "instagram", "eitaa", "whatsapp", "torob", "emalls",
            "sms", "email", "print", "exhibition", "partner");

    public static final java.util.List<String> DEFAULT_CAMPAIGN_MEDIUMS = java.util.List.of(
            "social", "cpc", "sms", "email", "qr", "print", "referral");

    private java.util.List<String> campaignSources;
    private java.util.List<String> campaignMediums;

    // ===============================
    // کدِ کالا و ایمپورتِ موجودی
    // ===============================

    /**
     * پیشوندِ کدِ کالا، مثلاً {@code DN-}.
     * <p>
     * 🔴 عمداً اینجاست و نه در کد: این دو حرف نامِ همین شرکت است و در یک اپِ
     * قابلِ‌استفادهٔ مجدد نباید هاردکد شود. نال یعنی پیشوندِ خنثایِ سرویس.
     */
    private String holooCodePrefix;

    /** تعدادِ ارقامِ کد (پیش‌فرضِ سرویس ۴). */
    private Integer holooCodeDigits;

    /**
     * آستانهٔ گاردِ صفرشدنِ انبوه: اگر یک ایمپورت بخواهد بیش از این درصد از محصولاتِ
     * کددار را صفر کند، بدونِ تأییدِ صریح رد می‌شود.
     * <p>
     * ⚠️ عدد در تنظیمات است نه در کد، چون «چند درصد مشکوک است» به اندازهٔ کاتالوگ و
     * عادتِ انبار بستگی دارد. نال یعنی پیش‌فرضِ سرویس.
     */
    private Integer stockImportMassZeroPercent;

    /** سقفِ تعدادِ پیامکِ «موجود شد» در یک ایمپورت. بیشتر از این، ایمپورت می‌ایستد و می‌پرسد. */
    private Integer stockImportSmsCap;

    public String getHolooCodePrefix() { return holooCodePrefix; }
    public void setHolooCodePrefix(String holooCodePrefix) { this.holooCodePrefix = holooCodePrefix; }

    public Integer getHolooCodeDigits() { return holooCodeDigits; }
    public void setHolooCodeDigits(Integer holooCodeDigits) { this.holooCodeDigits = holooCodeDigits; }

    public Integer getStockImportMassZeroPercent() { return stockImportMassZeroPercent; }
    public void setStockImportMassZeroPercent(Integer v) { this.stockImportMassZeroPercent = v; }

    public Integer getStockImportSmsCap() { return stockImportSmsCap; }
    public void setStockImportSmsCap(Integer v) { this.stockImportSmsCap = v; }

    public java.util.List<String> getCampaignSources() { return campaignSources; }
    public void setCampaignSources(java.util.List<String> campaignSources) { this.campaignSources = campaignSources; }

    public java.util.List<String> getCampaignMediums() { return campaignMediums; }
    public void setCampaignMediums(java.util.List<String> campaignMediums) { this.campaignMediums = campaignMediums; }

    public java.util.List<String> effectiveCampaignSources() {
        return (campaignSources == null || campaignSources.isEmpty())
                ? DEFAULT_CAMPAIGN_SOURCES : campaignSources;
    }

    public java.util.List<String> effectiveCampaignMediums() {
        return (campaignMediums == null || campaignMediums.isEmpty())
                ? DEFAULT_CAMPAIGN_MEDIUMS : campaignMediums;
    }

    // Getters & Setters

    public BigDecimal getRfqThreshold() { return rfqThreshold; }
    public void setRfqThreshold(BigDecimal rfqThreshold) { this.rfqThreshold = rfqThreshold; }

    public BigDecimal getSitePriceFactor() { return sitePriceFactor; }
    public void setSitePriceFactor(BigDecimal sitePriceFactor) { this.sitePriceFactor = sitePriceFactor; }

    /** ضریبِ مؤثر — اگر ست نشده یا نامعتبر بود، پیش‌فرض. */
    public BigDecimal effectiveSitePriceFactor() {
        return (sitePriceFactor != null && sitePriceFactor.compareTo(BigDecimal.ZERO) > 0)
                ? sitePriceFactor : DEFAULT_SITE_PRICE_FACTOR;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
}