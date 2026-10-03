package org.example.shop1.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * یک آیتم در درخواست مرتب‌سازی/جابجایی دسته‌ها (درگ‌دراپ).
 * فرانت‌اند کل ساختار درخت را به صورت لیستی از این آیتم‌ها می‌فرستد.
 * <p>
 * ⚠️ {@code parentId} سه‌حالته است، مثلِ {@link CategoryRequestDto}: کلیدِ نیامده یعنی
 * «والد دست نخورد»، {@code null}ِ صریح یعنی «ریشه». پیش از این، آیتمی که کلید را
 * نداشت بی‌صدا به ریشه می‌رفت. پنلِ ادمین همیشه کلید را می‌فرستد (ریشه‌ها با
 * {@code null}ِ صریح)، پس رفتارِ پنل عوض نشده است.
 */
public class CategoryOrderDto {

    private String id;
    private String parentId; // null یعنی ریشه
    private Integer position; // ترتیب بین هم‌ردیف‌ها

    @JsonIgnore
    private boolean parentIdPresent;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; this.parentIdPresent = true; }

    /** آیا کلیدِ {@code parentId} در بدنه آمده بود (حتی با {@code null})؟ */
    public boolean hasParentId() { return parentIdPresent; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
}
