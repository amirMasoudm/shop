package org.example.shop1.model.entity;

/**
 * یک ردیف از سرفصلِ دوره (تعبیه‌شده در Course).
 * group اختیاری است — ردیف‌های هم‌گروه زیر یک سرتیتر نمایش داده می‌شوند
 * (مثلاً: «روزِ اول»، «بخشِ عملی»). دقیقاً هم‌ساختارِ TechSpecRow است — سیستمِ
 * ادمینِ آن (ردیفِ افزودنی/حذف‌شدنی) عیناً برایِ سرفصل هم استفاده می‌شود.
 */
public class SyllabusItem {

    private String group;
    private String title;

    public SyllabusItem() {}

    public SyllabusItem(String group, String title) {
        this.group = group;
        this.title = title;
    }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
