package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
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

    // برای مدیریت حذف دسته: پیدا کردن/شمارش محصولات یک زیردرخت دسته‌بندی
    List<Product> findByCategoryIdIn(List<String> categoryIds);
    long countByCategoryIdIn(List<String> categoryIds);

    List<Product> findByWarehouseCategoryIdIn(List<String> categoryIds);
    long countByWarehouseCategoryIdIn(List<String> categoryIds);

    // متد مورد نیاز برای بررسی یونیک بودن ID توسط سرویس
    boolean existsById(String id);
    // پیدا کردن محصول از روی اسلاگ
    Optional<Product> findBySlug(String slug);

    /**
     * شمارشِ محصولاتِ «کامل» — معیارِ ثبت‌شدهٔ پروژه: {@code techSpecs} و {@code faqs} و
     * {@code description} هر سه غیرخالی.
     * <p>
     * ⚠️ چرا {@code count()} خالی به‌درد نمی‌خورد: صفحهٔ «سابقه و اعتبار» ادعا می‌کند این
     * محصولات مشخصاتِ فنی و پرسشِ متداول دارند. {@code count()} همهٔ اسناد را می‌شمارد،
     * پس با هر محصولِ خامِ تازه، ادعا رو به بالا از واقعیت جدا می‌شود.
     */
    @Query(value = "{ 'techSpecs': { $exists: true, $not: { $size: 0 } },"
            + " 'faqs': { $exists: true, $not: { $size: 0 } },"
            + " 'description': { $exists: true, $nin: [ null, '' ] } }", count = true)
    long countComplete();

    // پیدا کردن محصولاتی که آیدی آن‌ها در لیست ارسالی است
    List<Product> findByIdIn(List<String> productIds);

    // پیدا کردن محصولاتی که یک sectionId خاص را دارند (برای پاکسازی)
    List<Product> findBySectionIdsContaining(String sectionId);

    // نسخه صفحه‌بندی‌شده برای فرانت (جشنواره‌ها بدون نیاز به لود همه محصولات)
    Page<Product> findBySectionIdsContaining(String sectionId, Pageable pageable);

    // نمونه‌ای از فیلترینگ ترکیبی (پیاده‌سازی در سرویس):
    // Page<Product> findByCategoryIdAndDiscountedPriceBetween(String categoryId, BigDecimal min, BigDecimal max, Pageable pageable);
}