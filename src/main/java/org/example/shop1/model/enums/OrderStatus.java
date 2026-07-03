package org.example.shop1.model.enums;

public enum OrderStatus {
    PENDING_PAYMENT,    // سفارش باز (پرداخت نشده - کاربر امکان ویرایش و پرداخت دارد)
    PAID_PREPARING,     // در حال پردازش و بسته‌بندی در فروشگاه (به محض پرداخت موفق)
    POST_PROCESSING,    // پردازش در مرکز پستی
    SHIPPED,            // در حال ارسال به وسیله پست / پیک
    DELIVERED           // تحویل داده شده (پایان فرآیند)
}