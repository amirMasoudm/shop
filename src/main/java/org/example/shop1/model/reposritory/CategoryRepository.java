package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface CategoryRepository extends MongoRepository<Category, String> {
    List<Category> findByParentId(String parentId);

    // متد حیاتی برای فیلتر کردن
    List<Category> findByType(String type);
}