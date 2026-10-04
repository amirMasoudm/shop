package org.example.shop1.model.enums;

/**
 * نوعِ دستگاه — از User-Agent <b>مشتق</b> می‌شود.
 * <p>
 * رشتهٔ خامِ User-Agent عمداً هرگز ذخیره نمی‌شود: هم اثرانگشتِ کاربر است، هم برایِ
 * تصمیمِ تجاری («چند درصد موبایل‌اند») هیچ چیزی بیشتر از همین سه مقدار نمی‌دهد.
 */
public enum Device {
    MOBILE,
    DESKTOP,
    TABLET
}
