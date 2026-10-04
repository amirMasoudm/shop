package org.example.shop1.model.service;

import org.example.shop1.model.dto.ProductRequest;
import org.example.shop1.model.entity.Category;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.reposritory.CategoryRepository;
import org.example.shop1.model.reposritory.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 🔴 صدورِ کد فقط روی <b>ساخت</b> می‌نشیند، نه روی ذخیره.
 * <p>
 * اگر این قاعده بشکند هیچ خطایی نمی‌دهد: فقط هر ویرایشِ یک محصولِ موجود یک شماره
 * می‌سوزاند و — بدتر — کدی که شرکت در نرم‌افزارِ حسابداری روی همان کالا تایپ کرده
 * بی‌صاحب می‌شود و همگام‌سازیِ بعدی آن کالا را پیدا نمی‌کند. سابقهٔ همین اشتباه در
 * تسکِ {@code updateproduct-fix} ثبت است.
 */
class ProductHolooCodeTest {

    private ProductRepository productRepo;
    private HolooCodeService holooCodeService;
    private ProductService service;
    private final Map<String, Product> db = new HashMap<>();
    private final AtomicInteger issued = new AtomicInteger();

    private static final String CATEGORY_ID = "c-1";

    @BeforeEach
    void setUp() {
        productRepo = mock(ProductRepository.class);
        CategoryRepository categoryRepo = mock(CategoryRepository.class);
        holooCodeService = mock(HolooCodeService.class);
        service = new ProductService(productRepo, categoryRepo, mock(StockNotificationService.class),
                holooCodeService, mock(ActivityLogService.class));

        AtomicInteger ids = new AtomicInteger();
        when(productRepo.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            if (p.getId() == null) p.setId("p" + ids.incrementAndGet());
            db.put(p.getId(), p);
            return p;
        });
        when(productRepo.findById(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(db.get(inv.getArgument(0, String.class))));
        when(productRepo.findBySlug(anyString())).thenReturn(Optional.empty());
        when(productRepo.existsById(anyString()))
                .thenAnswer(inv -> db.containsKey(inv.getArgument(0, String.class)));

        when(holooCodeService.assignForNewProduct(anyString(), any()))
                .thenAnswer(inv -> String.format("DN-%04d", issued.incrementAndGet()));

        Category category = new Category();
        category.setId(CATEGORY_ID);
        when(categoryRepo.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
    }

    private ProductRequest req(String name) {
        ProductRequest r = new ProductRequest();
        r.setName(name);
        r.setSlug(name);
        r.setCategoryId(CATEGORY_ID);
        return r;
    }

    @Test
    void ساختِ_محصول_کد_صادر_می‌کند() {
        Product created = service.createProduct(req("کالای تازه"));

        assertEquals("DN-0001", created.getHolooCode());
        verify(holooCodeService).assignForNewProduct(created.getId(), null);
    }

    @Test
    void کدِ_رزروشدهٔ_انتخاب‌شده_به_سرویس_پاس_داده_می‌شود() {
        ProductRequest r = req("کالای تازه");
        r.setHolooCode("DN-0042");

        service.createProduct(r);

        verify(holooCodeService).assignForNewProduct(anyString(), eq("DN-0042"));
    }

    @Test
    void ذخیرهٔ_محصولِ_موجود_کدِ_تازه_صادر_نمی‌کند_و_کدِ_فعلی_را_عوض_نمی‌کند() {
        Product created = service.createProduct(req("کالا"));
        String original = created.getHolooCode();
        clearInvocations(holooCodeService);

        Product updated = service.updateProduct(created.getId(), req("کالا با نامِ تازه"));

        assertEquals(original, updated.getHolooCode(), "کد نباید عوض شود");
        verifyNoInteractions(holooCodeService);
        assertEquals(1, issued.get(), "هیچ شماره‌ای نباید سوخته باشد");
    }

    @Test
    void کدِ_ارسالی_در_ویرایش_نادیده_گرفته_می‌شود() {
        Product created = service.createProduct(req("کالا"));
        String original = created.getHolooCode();

        ProductRequest r = req("کالا");
        r.setHolooCode("DN-9999");   // تلاش برای جابه‌جاکردنِ کد از راهِ یک PUT
        Product updated = service.updateProduct(created.getId(), r);

        assertEquals(original, updated.getHolooCode());
    }

    @Test
    void ویرایشِ_دستیِ_موجودی_مُهرِ_زمان_می‌خورد() {
        Product created = service.createProduct(req("کالا"));
        assertNull(created.getStockTouchedManuallyAt(),
                "ساختِ بدونِ عددِ موجودی، ویرایشِ دستیِ موجودی نیست");

        ProductRequest withStock = req("کالا");
        withStock.setStockIsfahan(4);
        Product updated = service.updateProduct(created.getId(), withStock);

        assertNotNull(updated.getStockTouchedManuallyAt(),
                "بدونِ این مُهر، پیش‌نمایشِ ایمپورت نمی‌فهمد کدام ردیف کارِ دست را بازنویسی می‌کند");
        assertTrue(updated.isStockManuallyTouchedSinceImport());
    }
}
