package org.example.shop1.model.entity;

import org.example.shop1.model.enums.CourseMode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * دوره‌ی آموزشی — حضوری یا آنلاین (اسپات‌پلیر). طبقِ
 * docs/prompt-tech-chat-course-system.md موجودیتِ جداست، ولی از همان خطِ لولهٔ
 * فروشِ محصولات (Order/OrderItem/پرداختِ ملت) خریداری می‌شود — اینجا هیچ منطقِ
 * سبد/سفارش/پرداخت نیست، فقط دیتایِ خودِ دوره.
 * <p>
 * 🔴 startDate/endDate عمداً متنِ آزادند نه {@code Instant}: تاریخ‌هایِ این پروژه
 * شمسی‌اند و هیچ کتابخانه‌ی تبدیلِ شمسی/میلادی در پروژه نیست (دقیقاً همان تصمیمی
 * که برایِ CourseTrack و EducationArchiveItem.eventDate گرفته شد)؛ و چون وضعیتِ
 * «باز/تکمیل/غیرفعال» از رویِ isActive و ظرفیت محاسبه می‌شود نه از رویِ تاریخ،
 * نیازی به محاسبه‌ی واقعیِ تاریخ (مثلِ «آیا دوره شروع شده») نیست.
 */
@Document(collection = "courses")
public class Course {

    @Id
    private String id;

    private String title;

    private CourseMode mode = CourseMode.IN_PERSON;

    // عناوینِ آموزشی — فهرستِ سرفصل‌ها
    private List<String> syllabus = new ArrayList<>();

    private Integer theoryHours;
    private Integer practicalHours;

    private String instructor;

    private int capacity = 0;
    private int enrolledCount = 0;

    // مکانِ برگزاری — برایِ حضوری (شهر/آدرس)
    private String location;

    // متنِ آزاد (شمسی)، نه تاریخِ واقعی — نگاه کن به توضیحِ بالایِ کلاس
    private String startDate;
    private String endDate;

    private List<String> images = new ArrayList<>();
    private String bannerImage;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal price;

    private boolean isActive = true;

    private String description;
    private String slug;
    private String seoTitle;
    private String seoDescription;

    public Course() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public CourseMode getMode() { return mode; }
    public void setMode(CourseMode mode) { this.mode = mode; }

    public List<String> getSyllabus() { return syllabus; }
    public void setSyllabus(List<String> syllabus) { this.syllabus = syllabus; }

    public Integer getTheoryHours() { return theoryHours; }
    public void setTheoryHours(Integer theoryHours) { this.theoryHours = theoryHours; }

    public Integer getPracticalHours() { return practicalHours; }
    public void setPracticalHours(Integer practicalHours) { this.practicalHours = practicalHours; }

    public String getInstructor() { return instructor; }
    public void setInstructor(String instructor) { this.instructor = instructor; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public int getEnrolledCount() { return enrolledCount; }
    public void setEnrolledCount(int enrolledCount) { this.enrolledCount = enrolledCount; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public String getBannerImage() { return bannerImage; }
    public void setBannerImage(String bannerImage) { this.bannerImage = bannerImage; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getSeoTitle() { return seoTitle; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; }

    public String getSeoDescription() { return seoDescription; }
    public void setSeoDescription(String seoDescription) { this.seoDescription = seoDescription; }

    /** ظرفیتِ باقی‌مانده — منفی نمی‌شود (اگر enrolledCount به هر دلیلی از capacity رد شد). */
    public int getRemainingCapacity() {
        int remaining = capacity - enrolledCount;
        return Math.max(remaining, 0);
    }

    public boolean isFull() {
        return enrolledCount >= capacity;
    }
}
