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
 * تستِ باگِ دیتالاسِ {@code updateProduct}: PUTِ ناقص فیلدهایِ انبار را نال می‌کرد و
 * موجودیِ نال‌شده باعثِ NPE در PUTِ بعدی می‌شد (محصول برای همیشه غیرقابلِ‌آپدیت).
 * <p>
 * چرا تستِ واحد و نه تستِ HTTP: مسیرِ {@code PUT /api/v1/products} احرازِ هویتِ دومرحله‌ای
 * دارد و مرحلهٔ اول پیامکِ واقعی به شمارهٔ مالک می‌فرستد. برای تستِ خودکار قابلِ قبول نیست.
 */
class ProductServiceUpdateGuardTest {

    private ProductRepository productRepo;
    private CategoryRepository categoryRepo;
    private ProductService service;

    private static final String PRODUCT_ID = "p-1";
    private static final String CATEGORY_ID = "c-1";

    @BeforeEach
    void setUp() {
        productRepo = mock(ProductRepository.class);
        categoryRepo = mock(CategoryRepository.class);
        StockNotificationService notifications = mock(StockNotificationService.class);
        service = new ProductService(productRepo, categoryRepo, notifications, mock(HolooCodeService.class),
                mock(ActivityLogService.class));

        // save فقط همان شیء را برمی‌گرداند تا وضعیتِ نهایی قابلِ بازرسی باشد
        when(productRepo.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepo.findBySlug(anyString())).thenReturn(Optional.empty());
        Category category = new Category();
        category.setId(CATEGORY_ID);
        when(categoryRepo.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
    }

    /** محصولِ موجود با فیلدهایِ انبارِ پرشده — همان چیزی که پنلِ انبار ساخته است. */
    private Product existingProduct() {
        Product p = new Product();
        p.setId(PRODUCT_ID);
        p.setName("روتر میکروتیک نمونه");
        p.setCategoryId(CATEGORY_ID);
        p.setStock(7);
        p.setBasePrice(new BigDecimal("1000"));
        p.setWarehouseDescription("توضیحِ انبار");
        p.setUnit("عدد");
        p.setPackQuantity(12);
        p.setWarehouseCategoryId("wc-1");
        p.setWeight(2.5);
        p.setLength(30.0);
        p.setWidth(20.0);
        p.setHeight(10.0);
        return p;
    }

    /** PUTِ ناقصِ پنلِ فروشگاه: فقط نام و دسته — هیچ فیلدِ انبار/موجودی ندارد. */
    private ProductRequest partialStorefrontRequest() {
        ProductRequest r = new ProductRequest();
        r.setName("نامِ به‌روزشده");
        r.setCategoryId(CATEGORY_ID);
        return r;
    }

    @Test
    @DisplayName("PUTِ ناقص نباید موجودی را نال کند")
    void partialUpdateKeepsStock() {
        when(productRepo.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct()));

        Product saved = service.updateProduct(PRODUCT_ID, partialStorefrontRequest());

        assertNotNull(saved.getStock(), "موجودی نباید نال شود");
        assertEquals(7, saved.getStock(), "موجودیِ قبلی باید حفظ شود");
    }

    @Test
    @DisplayName("PUTِ ناقص نباید فیلدهایِ انبار را پاک کند")
    void partialUpdateKeepsWarehouseFields() {
        when(productRepo.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct()));

        Product saved = service.updateProduct(PRODUCT_ID, partialStorefrontRequest());

        assertEquals(new BigDecimal("1000"), saved.getBasePrice());
        assertEquals("توضیحِ انبار", saved.getWarehouseDescription());
        assertEquals("عدد", saved.getUnit());
        assertEquals(12, saved.getPackQuantity());
        assertEquals("wc-1", saved.getWarehouseCategoryId());
        assertEquals(2.5, saved.getWeight());
        assertEquals(30.0, saved.getLength());
        assertEquals(20.0, saved.getWidth());
        assertEquals(10.0, saved.getHeight());
    }

    @Test
    @DisplayName("رکوردِ قدیمیِ دارایِ stock نال نباید NPE بدهد")
    void updateOnNullStockRecordDoesNotThrow() {
        Product broken = existingProduct();
        broken.setStock(null); // قربانیِ باگِ قبلی
        when(productRepo.findById(PRODUCT_ID)).thenReturn(Optional.of(broken));

        ProductRequest request = partialStorefrontRequest();
        Product saved = assertDoesNotThrow(() -> service.updateProduct(PRODUCT_ID, request),
                "رکوردِ نال‌دار باید دوباره قابلِ آپدیت باشد");
        assertEquals(0, saved.getStock(), "نالِ کهنه باید به صفر (ناموجود) تفسیر شود");
    }

    @Test
    @DisplayName("مقدارِ صریحِ موجودی همچنان نوشته می‌شود")
    void explicitStockStillApplied() {
        when(productRepo.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct()));

        ProductRequest request = partialStorefrontRequest();
        request.setStock(0); // صفرِ عمدی = ناموجود، نباید با «ارسال‌نشده» اشتباه شود
        Product saved = service.updateProduct(PRODUCT_ID, request);

        assertEquals(0, saved.getStock(), "صفرِ صریح باید اعمال شود، نه نادیده گرفته");
    }

    @Test
    @DisplayName("مقدارِ صریحِ فیلدِ انبار همچنان نوشته می‌شود")
    void explicitWarehouseFieldStillApplied() {
        when(productRepo.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct()));

        ProductRequest request = partialStorefrontRequest();
        request.setUnit("بسته");
        request.setPackQuantity(24);
        Product saved = service.updateProduct(PRODUCT_ID, request);

        assertEquals("بسته", saved.getUnit());
        assertEquals(24, saved.getPackQuantity());
    }

    @Test
    @DisplayName("گذارِ ناموجود → موجود روی رکوردِ نال‌دار پیامک را تریگر می‌کند و نمی‌شکند")
    void backInStockTransitionSurvivesNullOldStock() {
        Product broken = existingProduct();
        broken.setStock(null);
        when(productRepo.findById(PRODUCT_ID)).thenReturn(Optional.of(broken));

        ProductRequest request = partialStorefrontRequest();
        request.setStock(5);
        Product saved = assertDoesNotThrow(() -> service.updateProduct(PRODUCT_ID, request));

        assertEquals(5, saved.getStock());
    }

    @Test
    @DisplayName("محصولِ تازه بدونِ موجودیِ ارسالی با صفر ساخته می‌شود، نه نال")
    void createWithoutStockDefaultsToZero() {
        when(productRepo.existsById(anyString())).thenReturn(false);

        ProductRequest request = new ProductRequest();
        request.setName("محصولِ تازه");
        request.setCategoryId(CATEGORY_ID);
        Product created = service.createProduct(request);

        assertNotNull(created.getStock(), "محصولِ تازه نباید با موجودیِ نال ساخته شود");
        assertEquals(0, created.getStock(), "پیش‌فرضِ امن: ناموجود");
    }
}
