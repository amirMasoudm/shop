package org.example.shop1.model.service;

import org.example.shop1.config.SecurityUtils;
import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.ActivityLog;
import org.example.shop1.model.entity.Article;
import org.example.shop1.model.reposritory.ArticleRepository;
import org.example.shop1.model.service.util.SlugUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ArticleService {

    private final ArticleRepository repo;
    private final ActivityLogService activityLog;

    public ArticleService(ArticleRepository repo, ActivityLogService activityLog) {
        this.repo = repo;
        this.activityLog = activityLog;
    }

    public Article save(Article input) {
        boolean isNew = input.getId() == null || input.getId().isBlank();

        Article article;
        if (isNew) {
            article = new Article();
            article.setCreatedAt(Instant.now());
        } else {
            article = repo.findById(input.getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "مقاله یافت نشد"));
        }

        if (input.getTitle() == null || input.getTitle().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "عنوان مقاله الزامی است");
        }

        // پاکسازی ورودی‌ها
        article.setTitle(SecurityUtils.clean(input.getTitle()));
        article.setExcerpt(SecurityUtils.clean(input.getExcerpt()));
        article.setSeoTitle(SecurityUtils.clean(input.getSeoTitle()));
        article.setSeoDescription(SecurityUtils.clean(input.getSeoDescription()));
        article.setCoverImage(SecurityUtils.clean(input.getCoverImage()));
        // محتوای مقاله: HTML غنی با Safelist ریلکس
        article.setContentHtml(SecurityUtils.cleanRich(input.getContentHtml()));
        // وضعیتِ پیش از تغییر، برایِ لاگِ انتشار — بعد از set دیگر دردسترس نیست.
        boolean wasPublished = !isNew && article.isPublished();
        article.setPublished(input.isPublished());
        article.setUpdatedAt(Instant.now());

        // خوشه‌ی محتوایی: اسلاگ مشترک از نام خوشه ساخته می‌شود
        String hub = SecurityUtils.clean(input.getHub());
        if (hub != null && !hub.trim().isEmpty()) {
            article.setHub(hub.trim());
            article.setHubSlug(SlugUtil.slugify(hub));
        } else {
            article.setHub(null);
            article.setHubSlug(null);
        }

        // اسلاگ: پایدار مثل دسته‌ها — فقط اگر صریحاً داده شود یا وجود نداشته باشد ساخته می‌شود
        if (input.getSlug() != null && !input.getSlug().trim().isEmpty()) {
            article.setSlug(generateUniqueSlug(input.getSlug(), article.getId()));
        } else if (article.getSlug() == null || article.getSlug().isEmpty()) {
            article.setSlug(generateUniqueSlug(article.getTitle(), article.getId()));
        }

        Article saved = repo.save(article);

        // ردپا — چهار نقشِ پنل روی محتوایِ عمومی دست می‌برند، پس باید معلوم باشد کی چه کرد.
        // ⚠️ انتشار جدا لاگ می‌شود و نه درونِ همان رکوردِ ویرایش: سؤالِ واقعی «چه کسی
        // این را منتشر کرد؟» است، نه «چه کسی دستش زد؟».
        if (isNew) {
            activityLog.record(ActivityLog.Action.ARTICLE_CREATE, ActivityLog.Source.MANUAL,
                    ActivityLogService.ENTITY_ARTICLE, saved.getId(), saved.getTitle(),
                    "article", null, saved.isPublished() ? "منتشرشده" : "پیش‌نویس");
        } else {
            activityLog.record(ActivityLog.Action.ARTICLE_UPDATE, ActivityLog.Source.MANUAL,
                    ActivityLogService.ENTITY_ARTICLE, saved.getId(), saved.getTitle(),
                    "article", null, null);
        }
        if (wasPublished != saved.isPublished()) {
            activityLog.record(ActivityLog.Action.ARTICLE_UPDATE, ActivityLog.Source.MANUAL,
                    ActivityLogService.ENTITY_ARTICLE, saved.getId(), saved.getTitle(),
                    "published",
                    wasPublished ? "منتشرشده" : "پیش‌نویس",
                    saved.isPublished() ? "منتشرشده" : "پیش‌نویس");
        }
        return saved;
    }

    public void delete(String id) {
        Article existing = repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "مقاله یافت نشد"));
        repo.deleteById(id);
        activityLog.record(ActivityLog.Action.ARTICLE_DELETE, ActivityLog.Source.MANUAL,
                ActivityLogService.ENTITY_ARTICLE, id, existing.getTitle(),
                "article", existing.getSlug(), null);
    }

    public List<Article> getAllForAdmin() {
        return repo.findAll();
    }

    // لیست عمومی بلاگ
    public Page<Article> getPublished(int page, int size) {
        return repo.findByPublishedTrueOrderByCreatedAtDesc(PageRequest.of(page, Math.min(size, 50)));
    }

    // صفحه‌ی عمومی مقاله: فقط منتشرشده
    public Optional<Article> getPublishedBySlugOrId(String slugOrId) {
        Optional<Article> found = repo.findBySlug(slugOrId);
        if (found.isEmpty()) found = repo.findById(slugOrId);
        return found.filter(Article::isPublished);
    }

    // برای sitemap
    public List<Article> getAllPublished() {
        return repo.findByPublishedTrue();
    }

    // مقالات یک خوشه (صفحه هاب + باکس مقالات مرتبط)
    public List<Article> getHubArticles(String hubSlug) {
        return repo.findByHubSlugAndPublishedTrueOrderByCreatedAtDesc(hubSlug);
    }

    // فقط تعدادِ مقالاتِ یک خوشه (بدون واکشیِ خودِ اسناد)
    public long countHubArticles(String hubSlug) {
        return repo.countByHubSlugAndPublishedTrue(hubSlug);
    }

    private String generateUniqueSlug(String base, String excludeId) {
        String slug = SlugUtil.slugify(base);
        if (slug.isEmpty()) slug = "article";

        String candidate = slug;
        int i = 2;
        while (true) {
            Optional<Article> existing = repo.findBySlug(candidate);
            if (existing.isEmpty() || existing.get().getId().equals(excludeId)) {
                return candidate;
            }
            candidate = slug + "-" + i++;
        }
    }
}
