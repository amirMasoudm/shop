package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * لاگِ فعالیتِ میزِ کارِ قیمت‌گذاری — «چه کسی، کِی، چه محصولی، از چه مقدار به چه مقدار،
 * از چه مسیری».
 * <p>
 * ⚠️ <b>حذف‌نشدنی است</b> (append-only): هیچ اندپوینتِ حذف/ویرایشی برایش وجود ندارد،
 * حتی برایِ ادمین. بدونِ این تضمین، پاسخگویی بی‌معنی است.
 */
@Document(collection = "activity_logs")
public class ActivityLog {

    /** نوعِ رویداد. */
    public enum Action {
        PRICE_CHANGE,        // تغییرِ قیمت (سایت یا همکار)
        FLOOR_PRICE_CHANGE,  // تغییرِ کفِ قیمتِ رقبا
        FLAG_CHANGE,         // تغییرِ پرچمِ «خیلی بفروشید»
        STOCK_CHANGE,        // اصلاحِ دستیِ موجودی از میزِ کار
        LOGIN,               // ورود به سیستم

        // ویرایشِ خودِ محصول از تبِ محصولات (نه میزِ کارِ قیمت‌گذاری). تا پیش از این
        // فقط تغییرِ قیمت لاگ می‌شد و ساخت/ویرایش/حذفِ محصول هیچ ردی نمی‌گذاشت.
        PRODUCT_CREATE,
        PRODUCT_UPDATE,
        PRODUCT_DELETE,

        // کنشِ کارمندی رویِ دادهٔ شخصیِ مشتریان. عمداً اینجاست و نه در user_events:
        // «چه کسی خروجیِ رفتارِ مشتریان را گرفت» خودش یک رویدادِ ممیزی است و باید
        // در لاگِ حذف‌نشدنی بنشیند، نه در لاگی که خودش بعد از ۹۰ روز پاک می‌شود.
        ANALYTICS_EXPORT,
        ANALYTICS_ERASE,

        // صدور/رزروِ کدِ کالا — عملی که برگشت ندارد، چون کد هرگز بازاستفاده نمی‌شود
        HOLOO_CODE_ISSUE,
        // یک دورِ ایمپورتِ موجودی از فایلِ حسابداری
        HOLOO_STOCK_IMPORT
    }

    /** مسیرِ انجامِ تغییر — برایِ تفکیکِ کارِ دستی از ورودِ دسته‌ای و (بعداً) هلو. */
    public enum Source {
        MANUAL,   // ویرایشِ دستی در میزِ کار
        DERIVED,  // محاسبه‌ی خودکار از فرمول (قیمتِ سایت از «فروش تعدادی»)
        BATCH,    // ورودِ دسته‌ای / ایمپورت
        HOLOO     // آینده: همگام‌سازیِ هلو
    }

    @Id
    private String id;

    @Indexed
    private String username;      // چه کسی

    @Indexed
    private Instant at = Instant.now();  // کِی

    private Action action;
    private Source source = Source.MANUAL;

    /**
     * نوعِ موجودیت: {@code PRODUCT}، {@code SETTINGS}، و بعداً {@code CATEGORY}،
     * {@code ARTICLE}، {@code USER} و…
     * <p>
     * عمداً عام است: قسمتِ (ب) لاگِ پنلِ ادمین قرار است رویدادهایی را ثبت کند که اصلاً
     * محصول نیستند. اگر schema محصول‌محور می‌ماند، آن تسک مجبور می‌شد یا مهاجرتِ دیتا
     * بزند یا کالکشنِ دومِ موازی بسازد (= دو صفحهٔ لاگ و دو منطقِ فیلتر).
     */
    @Indexed
    private String entityType;

    @Indexed
    private String entityId;      // شناسه‌ی موجودیت (برایِ LOGIN نال است)

    /** اسنپ‌شاتِ نامِ نمایشی، تا اگر موجودیت بعداً حذف شد لاگ همچنان خوانا بماند. */
    private String productName;

    private String field;         // کدام فیلد (onlinePrice, partnerBulkPrice, sitePriceFactor, ...)
    private String oldValue;      // از چه مقدار
    private String newValue;      // به چه مقدار

    public ActivityLog() {}

    public ActivityLog(String username, Action action, Source source,
                       String entityType, String entityId, String productName,
                       String field, String oldValue, String newValue) {
        this.username = username;
        this.action = action;
        this.source = source;
        this.entityType = entityType;
        this.entityId = entityId;
        this.productName = productName;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.at = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Instant getAt() { return at; }
    public void setAt(Instant at) { this.at = at; }

    public Action getAction() { return action; }
    public void setAction(Action action) { this.action = action; }

    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
}
