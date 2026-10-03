package org.example.shop1.model.enums;

/**
 * فرستندهٔ پیامِ چت از کدام سمت است.
 * <p>
 * عمداً از {@link Role} جداست: آن نقشِ دسترسیِ کاربر است (چه کاری مجاز است)، این
 * سمتِ گفت‌وگوست (پیام چپ رندر شود یا راست). یک کاربرِ ADMIN وقتی در چت جواب می‌دهد
 * {@code AGENT} است، نه ADMIN.
 */
public enum SenderRole {
    CUSTOMER,
    AGENT,

    /** پیامِ خودکارِ سیستم؛ فرستندهٔ انسانی ندارد. */
    SYSTEM
}
