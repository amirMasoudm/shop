package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends MongoRepository<Article, String> {

    Optional<Article> findBySlug(String slug);

    // لیست عمومی بلاگ (فقط منتشرشده‌ها)
    Page<Article> findByPublishedTrueOrderByCreatedAtDesc(Pageable pageable);

    // برای sitemap
    List<Article> findByPublishedTrue();

    // مقالات یک خوشه‌ی محتوایی (برای صفحه‌ی هاب و لینک‌های مرتبط)
    List<Article> findByHubSlugAndPublishedTrueOrderByCreatedAtDesc(String hubSlug);

    // شمارشِ سبکِ یک خوشه — برای بلاکِ «سابقه و اعتبار» در صفحه‌ی اصلی، تا هوم
    // مجبور نشود ۴۹ سندِ کامل (با contentHtml) را فقط برای گرفتنِ یک عدد بخواند.
    long countByHubSlugAndPublishedTrue(String hubSlug);
}
