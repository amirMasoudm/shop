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
        LOGIN                // ورود به سیستم
    }

    /** مسیرِ انجامِ تغییر — برایِ تفکیکِ کارِ دستی از ورودِ دسته‌ای و (بعداً) هلو. */
    public enum Source {
        MANUAL,   // ویرایشِ دستی در میزِ کار
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

    @Indexed
    private String productId;     // چه محصولی (برایِ LOGIN نال است)
    private String productName;   // اسنپ‌شاتِ نام، تا اگر محصول بعداً حذف شد لاگ خوانا بماند

    private String field;         // کدام فیلد (onlinePrice, partnerUnitPrice, ...)
    private String oldValue;      // از چه مقدار
    private String newValue;      // به چه مقدار

    public ActivityLog() {}

    public ActivityLog(String username, Action action, Source source,
                       String productId, String productName,
                       String field, String oldValue, String newValue) {
        this.username = username;
        this.action = action;
        this.source = source;
        this.productId = productId;
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

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
}
