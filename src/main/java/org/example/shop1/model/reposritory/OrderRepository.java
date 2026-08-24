package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.Order;
import org.example.shop1.model.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    // برای رفع خطای کامپایل در آمارگیری داشبورد ادمین
    List<Order> findByTypeAndOrderDateAfter(String type, Instant date);

    // برای لیست‌های ادمین
    List<Order> findByTypeOrderByOrderDateDesc(String type, Pageable pageable);

    // مهم: برای نمایش تاریخچه خرید در پنل کاربری (جایگزین فیلد Customer)
    List<Order> findByUserOrderByOrderDateDesc(User user);
    // متد جدید: جستجوی سفارشات بر اساس آیدی کاربر (بدون نیاز به لود کردن کل شیء User)
    List<Order> findByUserId(String userId);

    // callbackِ بانک شناسه‌ی داخلیِ Mongo را نمی‌شناسد، فقط orderIdِ عددی‌ای که خودمان
    // در bpPayRequest فرستادیم (paymentRefNumber) را برمی‌گرداند — پس با همین پیدا می‌شود.
    java.util.Optional<Order> findByPaymentRefNumber(Long paymentRefNumber);
}