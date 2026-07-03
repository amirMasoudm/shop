package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {


    // متد مورد نیاز برای پنل ادمین
    List<Product> findByCategoryId(String categoryId);

    // متد مورد نیاز برای فرانت‌اند (صفحه‌بندی بر اساس دسته‌بندی)
    Page<Product> findByCategoryId(String categoryId, Pageable pageable);
    // متد جدید: پیدا کردن محصولات اگر دسته‌بندی آن‌ها در لیست ارسالی باشد
    Page<Product> findByCategoryIdIn(List<String> categoryIds, Pageable pageable);

    // متد مورد نیاز برای بررسی یونیک بودن ID توسط سرویس
    boolean existsById(String id);
    // پیدا کردن محصول از روی اسلاگ
    Optional<Product> findBySlug(String slug);

    // پیدا کردن محصولاتی که آیدی آن‌ها در لیست ارسالی است
    List<Product> findByIdIn(List<String> productIds);

    // پیدا کردن محصولاتی که یک sectionId خاص را دارند (برای پاکسازی)
    List<Product> findBySectionIdsContaining(String sectionId);

    // نمونه‌ای از فیلترینگ ترکیبی (پیاده‌سازی در سرویس):
    // Page<Product> findByCategoryIdAndDiscountedPriceBetween(String categoryId, BigDecimal min, BigDecimal max, Pageable pageable);
}