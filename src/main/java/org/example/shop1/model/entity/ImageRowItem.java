package org.example.shop1.model.entity;

/**
 * یک عکسِ داخلِ یک ردیفِ بنرِ تصویری (تعبیه‌شده در ImageRowBanner).
 * link متنِ خام است — یا مسیرِ داخلی (با / شروع می‌شود) یا آدرسِ کاملِ خارجی
 * (با http شروع می‌شود)؛ تشخیصِ داخلی/خارجی سمتِ کلاینت با همین پیشوند انجام می‌شود.
 */
public class ImageRowItem {

    private String imageUrl;
    private String link;
    private String caption;

    public ImageRowItem() {}

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
}
