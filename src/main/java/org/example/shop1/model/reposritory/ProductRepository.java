package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    // متد مورد نیاز برای پنل ادمین
    List<Product> findByCategoryId(String categoryId);

    // متد مورد نیاز برای فرانت‌اند (صفحه‌بندی بر اساس دسته‌بندی)
    Page<Product> findByCategoryId(String categoryId, Pageable pageable);

    // متد مورد نیاز برای فرانت‌اند (صفحه‌بندی عمومی) - به طور ضمنی توسط findAll(Pageable) پشتیبانی می‌شود.

    // متد مورد نیاز برای بررسی یونیک بودن ID توسط سرویس
    boolean existsById(String id);

    // نمونه‌ای از فیلترینگ ترکیبی (پیاده‌سازی در سرویس):
    // Page<Product> findByCategoryIdAndDiscountedPriceBetween(String categoryId, BigDecimal min, BigDecimal max, Pageable pageable);
}