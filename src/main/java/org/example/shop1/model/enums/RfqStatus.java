package org.example.shop1.model.enums;

// وضعیت استعلام پیش‌فاکتور (RFQ)
public enum RfqStatus {
    PENDING,      // ثبت‌شده توسط کاربر، در انتظار قیمت‌دهی ادمین
    QUOTED,       // ادمین قیمت داد
    NEGOTIATING,  // کاربر پیشنهاد مقابل داد (چانه‌زنی)
    ACCEPTED,     // توافق نهایی و تبدیل به سفارش
    REJECTED,     // رد شده (توسط ادمین)
    EXPIRED       // منقضی‌شده
}
