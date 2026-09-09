package org.example.shop1.model.service;

import org.example.shop1.model.dto.ProductRequest;
import org.example.shop1.model.entity.Category;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.reposritory.CategoryRepository;
import org.example.shop1.model.reposritory.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * مستندسازیِ اجراییِ سه «تله»یِ {@code updateProduct} که عمداً اصلاح نشده‌اند.
 * <p>
 * این تست‌ها رفتارِ <b>فعلی</b> را تثبیت می‌کنند تا تیمِ محتوا بداند چه فیلدهایی را
 * حتماً باید در بدنهٔ PUT بفرستد. برخلافِ {@code ProductServiceUpdateGuardTest} که
 * یک باگ را می‌گیرد، اینجا هیچ باگی ادعا نمی‌شود — فقط رفتار ثبت می‌شود، چون هر سه
 * قبلاً باعثِ خرابیِ واقعی شده‌اند یا می‌توانند بشوند:
 * <ul>
 *   <li>{@code slug} نفرستادن ⇒ اسلاگ از نام بازتولید می‌شود ⇒ آدرسِ ایندکس‌شده می‌شکند.
 *       (یک‌بار رخ داد: ۱۴ محصول اسلاگِ بزرگ‌حروف گرفتند و ۴۰۴ دادند.)</li>
 *   <li>{@code discountPercent} نفرستادن ⇒ تخفیف صفر می‌شود.</li>
 *   <li>{@code categoryId} نفرستادن ⇒ استثنا.</li>
 * </ul>
 */
class ProductServiceUpdateSemanticsTest {

    private ProductRepository productRepo;
    private ProductService service;

    private static final String PRODUCT_ID = "p-1";
    private static final String CATEGORY_ID = "c-1";

    @BeforeEach
    void setUp() {
        productRepo = mock(ProductRepository.class);
        CategoryRepository categoryRepo = mock(CategoryRepository.class);
        service = new ProductService(productRepo, categoryRepo, mock(StockNotificationService.class),
                mock(ActivityLogService.class));

        when(productRepo.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepo.findBySlug(anyString())).thenReturn(Optional.empty());
        Category category = new Category();
        category.setId(CATEGORY_ID);
        when(categoryRepo.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
    }

    private Product existingProduct() {
        Product p = new Product();
        p.setId(PRODUCT_ID);
        p.setName("نامِ قدیمی");
        p.setSlug("aslag-e-ghadimi");
        p.setCategoryId(CATEGORY_ID);
        p.setStock(3);
        p.setPrice(new BigDecimal("1000"));
        p.setDiscountPercent(20);
        p.setDiscountedPrice(new BigDecimal("800.00"));
        return p;
    }

    @Test
    @DisplayName("تله ۱: تغییرِ نام بدونِ ارسالِ slug، اسلاگ را عوض می‌کند (آدرس می‌شکند)")
    void renameWithoutSlugRegeneratesSlug() {
        when(productRepo.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct()));

        ProductRequest r = new ProductRequest();
        r.setName("Mikrotik LHG 5 ax");
        r.setCategoryId(CATEGORY_ID);
        // slug عمداً فرستاده نمی‌شود

        Product saved = service.updateProduct(PRODUCT_ID, r);

        assertNotEquals("aslag-e-ghadimi", saved.getSlug(),
                "اسلاگ از نامِ تازه بازتولید می‌شود — آدرسِ قبلی می‌شکند");
        System.out.println("[تله ۱] اسلاگِ بازتولیدشده: " + saved.getSlug());
    }

    @Test
    @DisplayName("تله ۱ (پادزهر): ارسالِ slugِ فعلی آدرس را ثابت نگه می‌دارد")
    void renameWithExplicitSlugKeepsUrl() {
        when(productRepo.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct()));

        ProductRequest r = new ProductRequest();
        r.setName("نامِ کاملاً تازه");
        r.setCategoryId(CATEGORY_ID);
        r.setSlug("aslag-e-ghadimi"); // عیناً پس فرستاده می‌شود

        Product saved = service.updateProduct(PRODUCT_ID, r);

        assertEquals("aslag-e-ghadimi", saved.getSlug(), "با ارسالِ صریحِ slug آدرس تکان نمی‌خورد");
    }

    @Test
    @DisplayName("تله ۲: نفرستادنِ discountPercent تخفیف را صفر می‌کند")
    void missingDiscountPercentClearsDiscount() {
        when(productRepo.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct()));

        ProductRequest r = new ProductRequest();
        r.setName("نامِ قدیمی");
        r.setCategoryId(CATEGORY_ID);
        r.setPrice(new BigDecimal("1000"));
        // discountPercent عمداً فرستاده نمی‌شود

        Product saved = service.updateProduct(PRODUCT_ID, r);

        assertEquals(0, saved.getDiscountPercent(), "تخفیف فعالانه صفر می‌شود، نه اینکه حفظ شود");
    }

    @Test
    @DisplayName("تله ۳: نفرستادنِ categoryId استثنا می‌دهد")
    void missingCategoryIdThrows() {
        when(productRepo.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct()));

        ProductRequest r = new ProductRequest();
        r.setName("نامِ قدیمی");
        // categoryId عمداً فرستاده نمی‌شود

        Exception e = assertThrows(Exception.class, () -> service.updateProduct(PRODUCT_ID, r));
        System.out.println("[تله ۳] نوعِ استثنا: " + e.getClass().getName() + " — " + e.getMessage());
    }
}
