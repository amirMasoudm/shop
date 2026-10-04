package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface CommentRepository extends MongoRepository<Comment, String> {
    // برای نمایش در صفحه محصول (فقط تایید شده‌ها)
    List<Comment> findByProductIdAndStatus(String productId, String status);

    // برای پنل ادمین
    List<Comment> findByStatus(String status);

    List<Comment> findByUserId(String userId);
}