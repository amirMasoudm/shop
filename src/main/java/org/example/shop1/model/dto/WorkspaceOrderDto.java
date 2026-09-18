package org.example.shop1.model.dto;

/**
 * یک آیتم در درخواستِ مرتب‌سازیِ دستیِ ردیف‌های میزِ کارِ قیمت‌گذاری (درگ‌دراپ).
 * <p>
 * فرانت‌اند کلِ ترتیبِ جدید را به‌صورتِ لیستی از این آیتم‌ها می‌فرستد، نه فقط
 * ردیفِ جابه‌جاشده — چون جابه‌جاییِ یک ردیف، جایِ همهٔ ردیف‌هایِ بعد از خودش را
 * هم عوض می‌کند و فرستادنِ کلِ ترتیب ساده‌تر و بی‌ابهام‌تر است.
 * <p>
 * هم‌الگویِ {@link CategoryOrderDto} است تا کسی که یکی را خوانده دیگری را هم بفهمد.
 */
public class WorkspaceOrderDto {

    private String id;
    private Integer position;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
}
