package org.example.shop1.model.service;

import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.dto.ProductRequest;
import org.example.shop1.model.entity.Category;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.reposritory.CategoryRepository;
import org.example.shop1.model.reposritory.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * نگهبانِ قواعدِ ذخیرهٔ توقفِ تولید.
 * <p>
 * سه چیز اینجا آزموده می‌شود که شکستنشان بی‌صدا است:
 * <ul>
 *   <li><b>حلقه در لحظهٔ ذخیره</b> — «الف جایگزینش ب، ب جایگزینش الف» اگر ذخیره شود،
 *       هر دو صفحه بی‌مقصد می‌مانند و کارشناس نمی‌فهمد چرا ریدایرکت کار نمی‌کند.</li>
 *   <li><b>رشتهٔ خالی جایگزین را پاک می‌کند، ولی null دست نمی‌زند</b> — همان قاعدهٔ
 *       «فقط اگر ارسال شد» بقیهٔ فیلدها. اگر null هم پاک می‌کرد، ذخیره از فرمی که این
 *       فیلد را ندارد جایگزین را بی‌صدا می‌پراند.</li>
 *   <li><b>زنجیرهٔ سالم پذیرفته می‌شود</b> — جایگزینی که خودش متوقف است ولی به محصولِ
 *       زنده‌ای می‌رسد، خطا نیست.</li>
 * </ul>
 */
class ProductDiscontinuationTest {

    private ProductRepository productRepo;
    private ProductService service;
    private final Map<String, Product> db = new HashMap<>();

    private static final String CATEGORY_ID = "c-1";

    @BeforeEach
    void setUp() {
        productRepo = mock(ProductRepository.class);
        CategoryRepository categoryRepo = mock(CategoryRepository.class);
        service = new ProductService(productRepo, categoryRepo, mock(StockNotificationService.class),
                mock(ActivityLogService.class));

        when(productRepo.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            db.put(p.getId(), p);
            return p;
        });
        when(productRepo.findBySlug(anyString())).thenReturn(Optional.empty());
        when(productRepo.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(db.get(inv.getArgument(0, String.class))));
        when(productRepo.existsById(anyString())).thenAnswer(inv -> db.containsKey(inv.getArgument(0, String.class)));

        Category category = new Category();
        category.setId(CATEGORY_ID);
        when(categoryRepo.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
    }

    private Product put(String id, boolean discontinued, String replacement) {
        Product p = new Product();
        p.setId(id);
        p.setName("محصول " + id);
        p.setSlug("slug-" + id);
        p.setCategoryId(CATEGORY_ID);
        p.setStock(0);
        p.setPrice(new BigDecimal("1000"));
        p.setDiscontinued(discontinued);
        p.setReplacementProductId(replacement);
        db.put(id, p);
        return p;
    }

    /** بدنهٔ کمینهٔ PUT — slug و دسته صریح، تا تله‌های شناخته‌شدهٔ ویرایش دخالت نکنند. */
    private ProductRequest req(String id) {
        ProductRequest r = new ProductRequest();
        r.setName("محصول " + id);
        r.setSlug("slug-" + id);
        r.setCategoryId(CATEGORY_ID);
        return r;
    }

    @Test
    void توقف_با_جایگزین_و_پیام_ذخیره_می‌شود() {
        put("a", false, null);
        put("b", false, null);

        ProductRequest r = req("a");
        r.setDiscontinued(true);
        r.setReplacementProductId("b");
        r.setDiscontinuedNoticeVisible(true);
        Product saved = service.updateProduct("a", r);

        assertTrue(saved.isProductionStopped());
        assertEquals("b", saved.getReplacementProductId());
        assertTrue(saved.isDiscontinuedNoticeShown());
    }

    @Test
    void رشتهٔ_خالی_جایگزین_را_پاک_می‌کند_ولی_null_دست_نمی‌زند() {
        put("a", true, "b");
        put("b", false, null);

        ProductRequest untouched = req("a");          // جایگزین اصلاً فرستاده نشده
        assertEquals("b", service.updateProduct("a", untouched).getReplacementProductId(),
                "null یعنی «دست نزن» — ذخیره از فرمی که این فیلد را ندارد نباید جایگزین را بپراند");

        ProductRequest cleared = req("a");
        cleared.setReplacementProductId("");
        assertNull(service.updateProduct("a", cleared).getReplacementProductId(),
                "رشتهٔ خالی یعنی «بدونِ جایگزین»");
    }

    @Test
    void محصول_جایگزینِ_خودش_نمی‌شود() {
        put("a", false, null);
        ProductRequest r = req("a");
        r.setDiscontinued(true);
        r.setReplacementProductId("a");
        assertThrows(ApiException.class, () -> service.updateProduct("a", r));
    }

    @Test
    void جایگزینِ_ناموجود_رد_می‌شود() {
        put("a", false, null);
        ProductRequest r = req("a");
        r.setDiscontinued(true);
        r.setReplacementProductId("وجود-ندارد");
        assertThrows(ApiException.class, () -> service.updateProduct("a", r));
    }

    @Test
    void حلقه_در_لحظهٔ_ذخیره_گرفته_می‌شود() {
        put("a", false, null);
        put("b", true, "a");          // ب از قبل متوقف است و جایگزینش الف

        ProductRequest r = req("a");
        r.setDiscontinued(true);
        r.setReplacementProductId("b");   // الف → ب → الف
        ApiException e = assertThrows(ApiException.class, () -> service.updateProduct("a", r));
        assertTrue(e.getMessage().contains("حلقه"), "پیام باید علتِ رد را بگوید: " + e.getMessage());
        assertNull(db.get("a").getReplacementProductId(), "رکورد نباید نیمه‌کاره ذخیره شده باشد");
    }

    @Test
    void زنجیرهٔ_سالم_پذیرفته_می‌شود() {
        put("a", false, null);
        put("b", true, "c");          // ب متوقف، ولی به جِ زنده می‌رسد
        put("c", false, null);

        ProductRequest r = req("a");
        r.setDiscontinued(true);
        r.setReplacementProductId("b");
        assertEquals("b", service.updateProduct("a", r).getReplacementProductId());
    }
}
