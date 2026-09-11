package org.example.shop1.model.enums;

/**
 * رویدادهای رفتاریِ کاربر (کالکشنِ {@code user_events}).
 * <p>
 * ⚠️ این با {@code ActivityLog.Action} یکی نیست و نباید قاطی شود: آن لاگِ ممیزیِ
 * کارکنان است (حذف‌نشدنی، صدها رکورد)، این رفتارِ مشتری است (آماری، میلیون‌ها رکورد).
 */
public enum EventType {

    // ---- ناوبری ----
    /** شروعِ یک بازدید؛ تنها رویدادی که بستهٔ کاملِ منبعِ ورود را حمل می‌کند. */
    SESSION_START,
    PAGE_VIEW,

    // ---- کاتالوگ ----
    PRODUCT_VIEW,
    CATEGORY_VIEW,
    /** عبارت + تعدادِ نتیجه. جست‌وجوی بی‌نتیجه ارزشمندترین دادهٔ این سامانه است. */
    SEARCH,
    FILTER_APPLY,
    SORT_CHANGE,

    // ---- نیّتِ خرید ----
    ADD_TO_CART,
    REMOVE_FROM_CART,
    CART_VIEW,
    BEGIN_CHECKOUT,
    ORDER_PLACED,
    PAYMENT_RESULT,
    RFQ_SUBMIT,
    STOCK_NOTIFY_SUBSCRIBE,

    // ---- محتوا ----
    ARTICLE_VIEW,
    /** درصدِ مطالعه: ۲۵/۵۰/۷۵/۱۰۰ در props. */
    ARTICLE_READ_DEPTH,
    COURSE_VIEW,
    CATALOG_OPEN,

    // ---- پشتیبانی و حساب ----
    CHAT_OPEN,
    /** فقط «پیام فرستاد» — متنِ پیام اینجا ذخیره نمی‌شود، جایش کالکشنِ چت است. */
    CHAT_MESSAGE_SENT,
    LOGIN,
    LOGOUT,
    REGISTER,
    /** لحظه‌ای که یک anonIdِ ناشناس به یک کاربرِ واقعی دوخته می‌شود. */
    IDENTIFY
}
