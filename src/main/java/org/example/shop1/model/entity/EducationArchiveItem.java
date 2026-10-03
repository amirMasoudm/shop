package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * یک گروهِ عکسِ آرشیوِ تصویریِ کلاس‌ها/دوره‌های برگزارشده — برایِ پنجرهٔ صفحه‌ی داده‌نما و
 * صفحه‌ی آموزشِ کامل. طبقِ docs/prompt-tech-chat-education-gallery.md عمداً موجودیتِ
 * جداست (الگویِ دقیقِ {@link Banner})، نه توسعه‌ی بنر — این آرشیوِ داخلی است، نه بنرِ
 * تبلیغاتی، پس عمداً هیچ فیلدِ لینک ندارد.
 * <p>
 * 🔴 هر گروه چند عکس دارد ({@code imageUrls}) نه یک عکس — یک کلاس/دوره معمولاً چند
 * عکس دارد (عکسِ دسته‌جمعی، تخته، مراسمِ گواهی‌نامه و...)، پس آپلود و مدیریتِ عکس‌ها
 * گروهی است (خواسته‌ی صریحِ مالک).
 */
@Document(collection = "education_archive_items")
public class EducationArchiveItem {

    @Id
    private String id;

    // اجباری — زیرِ گروهِ عکس نمایش داده می‌شود
    private String title;

    // اجباری، حداقل یک عکس
    private List<String> imageUrls = new ArrayList<>();

    // اختیاری — توضیحِ کوتاه
    private String caption;

    // اختیاری، متنِ آزاد (نه تاریخِ واقعی) — چون تاریخ‌هایِ آرشیو شمسی و ناهمگون‌اند
    private String eventDate;

    private int orderIndex = 0;
    private boolean isActive = true;

    public EducationArchiveItem() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public String getEventDate() { return eventDate; }
    public void setEventDate(String eventDate) { this.eventDate = eventDate; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
