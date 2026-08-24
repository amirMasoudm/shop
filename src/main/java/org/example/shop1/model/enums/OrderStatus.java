package org.example.shop1.model.enums;

public enum OrderStatus {
    PENDING_PAYMENT,    // سفارش باز (پرداخت نشده - کاربر امکان ویرایش و پرداخت دارد)
    PAID_PREPARING,     // در حال پردازش و بسته‌بندی در فروشگاه (به محض پرداخت موفق)
    POST_PROCESSING,    // پردازش در مرکز پستی
    SHIPPED,            // در حال ارسال به وسیله پست / پیک
    DELIVERED,          // تحویل داده شده (پایان فرآیند)

    // پرداختِ واقعی (ملت) شکست خورد/لغو شد — تا این مقدار نبود، شکست خاموش می‌ماند
    // و کاربر/پشتیبانی هیچ‌جا نمی‌دید چرا سفارش هنوز PENDING_PAYMENT مانده.
    PAYMENT_FAILED
}