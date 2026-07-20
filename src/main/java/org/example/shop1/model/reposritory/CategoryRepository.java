package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends MongoRepository<Category, String> {
    List<Category> findByParentId(String parentId);

    // متد حیاتی برای فیلتر کردن
    List<Category> findByType(String type);

    // برای صفحه‌ی سئوی دسته (/category/{slug})
    Optional<Category> findBySlug(String slug);
}