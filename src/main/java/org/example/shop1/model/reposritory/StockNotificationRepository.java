package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.StockNotification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockNotificationRepository extends MongoRepository<StockNotification, String> {

    // برای جلوگیری از ثبتِ تکراریِ همان کاربر برای همان محصول (که هنوز اطلاع داده نشده)
    boolean existsByProductIdAndMobileAndNotifiedFalse(String productId, String mobile);

    // مشترکینِ اطلاع‌نداده‌شده‌ی یک محصول (برای تریگرِ موجودشدن)
    List<StockNotification> findByProductIdAndNotifiedFalse(String productId);
}
