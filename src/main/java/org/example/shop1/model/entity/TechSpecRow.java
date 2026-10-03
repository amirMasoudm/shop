package org.example.shop1.model.entity;

/**
 * یک ردیف از جدول مشخصات فنی محصول (تعبیه‌شده در Product).
 * group اختیاری است — ردیف‌های هم‌گروه زیر یک سرتیتر نمایش داده می‌شوند
 * (مثلاً: «مشخصات وایرلس»، «مشخصات سخت‌افزار»).
 * این جدول جدا از specifications است؛ specifications فقط کلیدهای فیلترپذیر دسته است.
 */
public class TechSpecRow {

    private String group;
    private String key;
    private String value;

    public TechSpecRow() {}

    public TechSpecRow(String group, String key, String value) {
        this.group = group;
        this.key = key;
        this.value = value;
    }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
