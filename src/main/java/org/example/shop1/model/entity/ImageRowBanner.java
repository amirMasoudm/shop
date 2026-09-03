package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * یک «پنجره»ی عنوان‌دار از عکس‌هایِ ردیفی، قابلِ‌کلیک (هر عکس لینک/عنوانِ خودش را
 * دارد، مثلِ Bannerِ اسلایدر ولی به‌جایِ چرخشِ تک‌عکسی، همه‌ی عکس‌ها کنارِ هم در یک
 * ردیفِ افقیِ اسکرول‌شدنی‌اند). placement مشخص می‌کند این پنجره کجایِ سایت بیاید
 * (مثلاً HOME یا SHOP) — همان الگویِ BannerPlacement، اینجا رشته‌ی ساده چون فعلاً
 * فقط دو جایگاه لازم است.
 */
@Document(collection = "image_row_banners")
public class ImageRowBanner {

    @Id
    private String id;

    private String title;
    private String placement;

    // فقط وقتی placement=SHOP_AFTER_SECTION پر می‌شود: شناسه‌ی سکشنِ لندینگی که این
    // پنجره زیرِ آن بیاید — دقیقاً همان الگویِ Banner.afterSectionId
    private String afterSectionId;

    private int position = 0;
    private boolean active = true;

    // نحوه‌ی نمایشِ ردیف: STATIC (اسکرولِ دستی — پیش‌فرض)، ROTATING (اسلایدرِ
    // یک‌عکسی با چرخشِ خودکار)، MARQUEE (نوارِ درحالِ‌حرکتِ پیوسته)
    private String displayMode = "STATIC";

    // فقط برایِ ROTATING/MARQUEE معنا دارد: ROTATING = فاصله‌ی چرخش (ثانیه)،
    // MARQUEE = مدتِ یک دورِ کاملِ حرکت (ثانیه — عددِ بزرگ‌تر یعنی کندتر)
    private int rotationSpeedSeconds = 5;

    private List<ImageRowItem> images = new ArrayList<>();

    public ImageRowBanner() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPlacement() { return placement; }
    public void setPlacement(String placement) { this.placement = placement; }

    public String getAfterSectionId() { return afterSectionId; }
    public void setAfterSectionId(String afterSectionId) { this.afterSectionId = afterSectionId; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getDisplayMode() { return displayMode; }
    public void setDisplayMode(String displayMode) { this.displayMode = displayMode; }

    public int getRotationSpeedSeconds() { return rotationSpeedSeconds; }
    public void setRotationSpeedSeconds(int rotationSpeedSeconds) { this.rotationSpeedSeconds = rotationSpeedSeconds; }

    public List<ImageRowItem> getImages() { return images; }
    public void setImages(List<ImageRowItem> images) { this.images = images; }
}
