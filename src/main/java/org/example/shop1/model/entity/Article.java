package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * مقاله‌ی بلاگ — زیرساخت قطب محتوای آموزشی (فاز ۳ رودمپ).
 * صفحه‌ی عمومی: /blog/{slug} (SSR کامل برای سئو)
 */
@Document(collection = "articles")
public class Article {

    @Id
    private String id;

    private String title;

    @Indexed(unique = true)
    private String slug;

    // خلاصه برای کارت‌ها و fallback توضیح متا
    private String excerpt;

    // محتوای HTML (خروجی ادیتور پنل — با Safelist ریلکس پاکسازی می‌شود)
    private String contentHtml;

    private String coverImage;

    // ---> خوشه‌ی محتوایی (Content Hub) <---
    // مقالات هم‌خوشه به هم لینک می‌شوند و صفحه‌ی /blog/hub/{hubSlug} می‌سازند
    private String hub;      // نام نمایشی خوشه، مثل: آموزش میکروتیک
    private String hubSlug;  // اسلاگ مشترک خوشه (از hub ساخته می‌شود)

    // ---> سئو <---
    private String seoTitle;
    private String seoDescription;

    private boolean published = false;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }

    public String getContentHtml() { return contentHtml; }
    public void setContentHtml(String contentHtml) { this.contentHtml = contentHtml; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

    public String getHub() { return hub; }
    public void setHub(String hub) { this.hub = hub; }

    public String getHubSlug() { return hubSlug; }
    public void setHubSlug(String hubSlug) { this.hubSlug = hubSlug; }

    public String getSeoTitle() { return seoTitle; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; }

    public String getSeoDescription() { return seoDescription; }
    public void setSeoDescription(String seoDescription) { this.seoDescription = seoDescription; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
