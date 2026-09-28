package org.example.shop1.model.service;

import org.bson.Document;
import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.Counter;
import org.example.shop1.model.entity.HolooCode;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.entity.StoreSettings;
import org.example.shop1.model.reposritory.HolooCodeRepository;
import org.example.shop1.model.reposritory.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.mongodb.client.result.UpdateResult;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * نگهبانِ صدورِ کد.
 * <p>
 * سه قاعده اینجا آزموده می‌شوند که شکستنشان ماه‌ها بعد و در قالبِ «موجودیِ یک کالا
 * روی کالای دیگر نشست» معلوم می‌شود:
 * <ul>
 *   <li><b>شمارنده اتمیک است</b> — دو درخواستِ هم‌زمان دو شماره می‌گیرند.</li>
 *   <li><b>کد هرگز بازاستفاده نمی‌شود</b> — رزروِ رهاشده آزاد نمی‌شود.</li>
 *   <li><b>مهاجرت بی‌اثر‌پذیر است و شمارنده را جلو می‌برد</b> — وگرنه محصولِ بعدی
 *       با محصولِ موجود تصادم می‌کند.</li>
 * </ul>
 */
class HolooCodeServiceTest {

    private HolooCodeService service;
    private ProductRepository productRepo;
    private StoreSettings settings;

    // ConcurrentHashMap و نه LinkedHashMap: تستِ صدورِ هم‌زمان واقعاً چند نخ می‌سازد
    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final Map<String, HolooCode> ledger = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong(0);

    @BeforeEach
    void setUp() {
        productRepo = mock(ProductRepository.class);
        HolooCodeRepository codeRepo = mock(HolooCodeRepository.class);
        StoreSettingsService settingsService = mock(StoreSettingsService.class);
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);

        settings = new StoreSettings();
        settings.setHolooCodePrefix("DN-");
        when(settingsService.getSettings()).thenReturn(settings);

        // شمارنده: دقیقاً همان معنایِ findAndModify + $inc در مونگو — یک عملیاتِ اتمیک
        when(mongoTemplate.findAndModify(any(), any(), any(), eq(Counter.class)))
                .thenAnswer(inv -> new Counter(HolooCodeService.COUNTER_NAME, counter.incrementAndGet()));
        when(mongoTemplate.findById(anyString(), eq(Counter.class)))
                .thenAnswer(inv -> new Counter(HolooCodeService.COUNTER_NAME, counter.get()));
        when(mongoTemplate.upsert(any(), any(Update.class), eq(Counter.class))).thenAnswer(inv -> {
            Update u = inv.getArgument(1);
            Document set = (Document) u.getUpdateObject().get("$set");
            counter.set(((Number) set.get("seq")).longValue());
            return null;
        });

        when(codeRepo.save(any(HolooCode.class))).thenAnswer(inv -> {
            HolooCode c = inv.getArgument(0);
            if (c.getId() == null) c.setId("id-" + c.getCode());
            ledger.put(c.getCode(), c);
            return c;
        });
        when(codeRepo.findByCode(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(ledger.get(inv.getArgument(0, String.class))));
        when(codeRepo.existsByCode(anyString()))
                .thenAnswer(inv -> ledger.containsKey(inv.getArgument(0, String.class)));
        when(codeRepo.findByStatusOrderByIssuedAtDesc(any())).thenAnswer(inv -> {
            HolooCode.Status wanted = inv.getArgument(0);
            List<HolooCode> out = new ArrayList<>();
            for (HolooCode c : ledger.values()) if (c.getStatus() == wanted) out.add(c);
            return out;
        });

        // شبیه‌سازیِ نوشتنِ شرطیِ مونگو: «فقط اگر holooCode هنوز خالی است».
        // synchronized نقشِ اتمیک‌بودنِ خودِ updateFirst را بازی می‌کند، و پرتابِ
        // DuplicateKeyException نقشِ ایندکسِ یکتا روی products.holooCode را.
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Product.class)))
                .thenAnswer(inv -> {
                    Query q = inv.getArgument(0);
                    Update u = inv.getArgument(1);
                    String id = String.valueOf(q.getQueryObject().get("_id"));
                    Document set = (Document) u.getUpdateObject().get("$set");
                    String code = String.valueOf(set.get("holooCode"));
                    synchronized (products) {
                        Product p = products.get(id);
                        if (p == null) return UpdateResult.acknowledged(0, 0L, null);
                        if (p.getHolooCode() != null) return UpdateResult.acknowledged(1, 0L, null);
                        boolean taken = products.values().stream()
                                .anyMatch(x -> code.equals(x.getHolooCode()));
                        if (taken) throw new DuplicateKeyException("uk_products_holooCode");
                        p.setHolooCode(code);
                        return UpdateResult.acknowledged(1, 1L, null);
                    }
                });

        when(productRepo.existsByHolooCode(anyString())).thenAnswer(inv -> {
            String code = inv.getArgument(0);
            return products.values().stream().anyMatch(p -> code.equals(p.getHolooCode()));
        });
        when(productRepo.findById(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(products.get(inv.getArgument(0, String.class))));
        when(productRepo.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            products.put(p.getId(), p);
            return p;
        });

        service = new HolooCodeService(mongoTemplate, codeRepo, productRepo, settingsService,
                mock(ActivityLogService.class));
    }

    private Product product(String id) {
        Product p = new Product();
        p.setId(id);
        p.setName("محصول " + id);
        products.put(id, p);
        return p;
    }

    // ==========================================================

    @Test
    void قالبِ_کد_از_تنظیمات_می‌آید_نه_از_کد() {
        assertEquals("DN-0001", service.format(1));
        assertEquals("DN-0229", service.format(229));

        settings.setHolooCodePrefix(null);
        assertEquals(HolooCodeService.FALLBACK_PREFIX + "0001", service.format(1),
                "نصبِ تازه بدونِ تنظیمات با پیشوندِ خنثی کار می‌کند، نه با پیشوندِ یک فروشگاهِ خاص");
    }

    @Test
    void تشخیصِ_الگوی_کد_مستقل_از_پیشوند_است() {
        assertTrue(HolooCodeService.looksLikeCode("DN-0001"));
        assertTrue(HolooCodeService.looksLikeCode("SKU-12345"));
        assertFalse(HolooCodeService.looksLikeCode("hEX S"));
        assertFalse(HolooCodeService.looksLikeCode("RBwAPR-2nD&R11e-LTE"));
        assertEquals(229, HolooCodeService.sequenceOf("DN-0229"));
        assertEquals("DN-", HolooCodeService.prefixOf("DN-0229"));
    }

    @Test
    void رزرو_بدونِ_یادداشت_رد_می‌شود() {
        ApiException ex = assertThrows(ApiException.class, () -> service.reserve("  "));
        assertTrue(ex.getMessage().contains("یادداشت"), ex.getMessage());
    }

    @Test
    void رزرو_کد_می‌دهد_و_در_فهرستِ_بی‌صاحب_می‌نشیند() {
        HolooCode reserved = service.reserve("سوئیچ سیسکو ۲۹۶۰");

        assertEquals("DN-0001", reserved.getCode());
        assertEquals(HolooCode.Status.RESERVED, reserved.getStatus());
        assertEquals(1, service.openReservations().size());
    }

    @Test
    void محصولِ_ساخته‌شده_با_کدِ_رزروشده_همان_کد_را_می‌گیرد() {
        HolooCode reserved = service.reserve("همان سوئیچ");
        product("p1");

        String assigned = service.assignForNewProduct("p1", reserved.getCode());

        assertEquals(reserved.getCode(), assigned, "نباید کدِ تازه بگیرد");
        assertEquals(HolooCode.Status.ASSIGNED, ledger.get(assigned).getStatus());
        assertEquals("p1", ledger.get(assigned).getProductId());
        assertTrue(service.openReservations().isEmpty(), "رزرو باید مصرف‌شده حساب شود");
    }

    @Test
    void کدِ_مصرف‌شده_دوباره_به_محصولِ_دیگر_داده_نمی‌شود() {
        HolooCode reserved = service.reserve("یکی");
        product("p1");
        product("p2");
        service.assignForNewProduct("p1", reserved.getCode());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.assignForNewProduct("p2", reserved.getCode()));
        assertTrue(ex.getMessage().contains("قبلاً"), ex.getMessage());
    }

    @Test
    void کدِ_صادرنشده_پذیرفته_نمی‌شود() {
        product("p1");
        ApiException ex = assertThrows(ApiException.class,
                () -> service.assignForNewProduct("p1", "DN-7777"));
        assertTrue(ex.getMessage().contains("صادر نشده"), ex.getMessage());
    }

    @Test
    void رزروِ_رهاشده_آزاد_نمی‌شود() {
        service.reserve("کدی که هیچ‌وقت استفاده نشد");   // DN-0001 می‌سوزد
        product("p1");

        String next = service.assignForNewProduct("p1", null);

        assertEquals("DN-0002", next,
                "کدِ رهاشده نباید دوباره صادر شود — ممکن است شرکت آن را در هلو تایپ کرده باشد");
    }

    @Test
    void دو_درخواستِ_هم‌زمان_دو_شمارهٔ_متفاوت_می‌گیرند() throws Exception {
        int threads = 20;
        Set<String> codes = ConcurrentHashMap.newKeySet();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    codes.add(service.reserve("هم‌زمان").getCode());
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertEquals(threads, codes.size(), "هیچ دو درخواستی نباید یک شماره بگیرند");
    }

    @Test
    void شمارنده_عقب_نمی‌رود() {
        service.raiseSequenceTo(228);
        assertEquals(228, service.currentSequence());

        service.raiseSequenceTo(5);
        assertEquals(228, service.currentSequence(), "عقب‌بردن یعنی صدورِ دوبارهٔ کدِ سوخته");
    }

    // ==========================================================
    // صدور برایِ محصولی که کد ندارد
    // ==========================================================

    @Test
    void محصولِ_بی‌کد_کدِ_تازه_می‌گیرد() {
        service.raiseSequenceTo(228);
        product("p1");

        String code = service.assignToExistingProduct("p1", null);

        assertEquals("DN-0229", code);
        assertEquals("DN-0229", products.get("p1").getHolooCode());
        assertEquals(HolooCode.Status.ASSIGNED, ledger.get(code).getStatus());
        assertEquals("p1", ledger.get(code).getProductId());
    }

    @Test
    void محصولی_که_کد_دارد_بارِ_دوم_تعارض_می‌گیرد_و_کدش_عوض_نمی‌شود() {
        product("p1");
        String first = service.assignToExistingProduct("p1", null);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.assignToExistingProduct("p1", null));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains(first), ex.getMessage());
        assertEquals(first, products.get("p1").getHolooCode(), "کدِ فعلی نباید بازنویسی شود");
    }

    @Test
    void محصولِ_نبود_۴۰۴_می‌گیرد() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.assignToExistingProduct("نیست", null));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void صدور_با_کدِ_رزروشده_رزرو_را_مصرف_می‌کند() {
        HolooCode reserved = service.reserve("سوئیچ سیسکو");
        product("p1");

        String code = service.assignToExistingProduct("p1", reserved.getCode());

        assertEquals(reserved.getCode(), code);
        assertEquals(code, products.get("p1").getHolooCode());
        assertTrue(service.openReservations().isEmpty(), "رزرو باید مصرف‌شده شود");
    }

    @Test
    void صدور_با_کدی_که_رزرو_نیست_رد_می‌شود() {
        product("p1");
        product("p2");
        String taken = service.assignToExistingProduct("p1", null);

        ApiException unknown = assertThrows(ApiException.class,
                () -> service.assignToExistingProduct("p2", "DN-7777"));
        assertTrue(unknown.getMessage().contains("صادر نشده"), unknown.getMessage());

        ApiException consumed = assertThrows(ApiException.class,
                () -> service.assignToExistingProduct("p2", taken));
        assertTrue(consumed.getMessage().contains("رزروِ مصرف‌نشده نیست"), consumed.getMessage());
        assertNull(products.get("p2").getHolooCode());
    }

    /**
     * 🔴 مسابقه‌ای که ایندکسِ یکتا نمی‌گیرد.
     * <p>
     * دو درخواستِ هم‌زمان روی یک محصولِ بی‌کد، دو شمارهٔ <b>متفاوت</b> از شمارنده
     * می‌گیرند، پس هیچ کلیدِ تکراری‌ای رخ نمی‌دهد و بدونِ نوشتنِ شرطی دومی بی‌صدا
     * اولی را بازمی‌نویسد — یعنی یک کدِ سوخته که در دفتر به محصولی اشاره می‌کند که
     * دیگر آن را ندارد. شرطِ {@code holooCode == null} داخلِ خودِ کوئری این را می‌بندد.
     */
    @Test
    void دو_صدورِ_هم‌زمان_روی_یک_محصول_یکی_موفق_یکی_خطای_روشن() throws Exception {
        product("p1");
        int threads = 8;
        java.util.concurrent.atomic.AtomicInteger ok = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger conflict = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger other = new java.util.concurrent.atomic.AtomicInteger();

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    service.assignToExistingProduct("p1", null);
                    ok.incrementAndGet();
                } catch (ApiException e) {
                    if (e.getStatus() == HttpStatus.CONFLICT) conflict.incrementAndGet();
                    else other.incrementAndGet();
                } catch (Exception e) {
                    other.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertEquals(1, ok.get(), "فقط یکی باید موفق شود");
        assertEquals(threads - 1, conflict.get(), "بقیه باید تعارضِ روشن بگیرند، نه ۵۰۰");
        assertEquals(0, other.get(), "هیچ خطای ناشناخته‌ای نباید رخ دهد");
        assertNotNull(products.get("p1").getHolooCode());
    }

    // ==========================================================
    // مهاجرت
    // ==========================================================

    private static final String CSV = """
            holooCode,productId,name,mikrotikCode,category,slug
            DN-0001,p1,کالای اول,,آنتن,alef
            DN-0002,p2,کالای دوم,,آنتن,be
            DN-0228,p3,کالای آخر,,آنتن,akhar
            """;

    @Test
    void مهاجرت_کدها_را_می‌نشاند_و_شمارنده_را_جلو_می‌برد() {
        product("p1");
        product("p2");
        product("p3");

        var result = service.migrateFromCsv(CSV);

        assertEquals(3, result.rows());
        assertEquals(3, result.assigned());
        assertEquals("DN-0001", products.get("p1").getHolooCode());
        assertEquals("DN-0228", products.get("p3").getHolooCode());
        assertEquals(228, result.counterAfter(), "🔴 بدونِ این، محصولِ بعدی DN-0001 می‌گیرد");
    }

    @Test
    void محصولِ_تازه_بعد_از_مهاجرت_کدِ_۰۲۲۹_می‌گیرد() {
        product("p1");
        product("p2");
        product("p3");
        service.migrateFromCsv(CSV);

        product("p-new");
        assertEquals("DN-0229", service.assignForNewProduct("p-new", null));
    }

    @Test
    void اجرای_دوبارهٔ_مهاجرت_هیچ_تغییری_نمی‌دهد() {
        product("p1");
        product("p2");
        product("p3");
        service.migrateFromCsv(CSV);
        long counterAfterFirst = service.currentSequence();

        var second = service.migrateFromCsv(CSV);

        assertEquals(0, second.assigned(), "بار دوم هیچ کدی نباید نشانده شود");
        assertEquals(3, second.alreadyCorrect());
        assertEquals(0, second.conflicts());
        assertEquals(counterAfterFirst, service.currentSequence(), "شمارنده نباید تکان بخورد");
    }

    @Test
    void مهاجرت_کدِ_موجود_را_عوض_نمی‌کند_و_تعارض_را_گزارش_می‌دهد() {
        product("p1").setHolooCode("DN-9999");
        product("p2");
        product("p3");

        var result = service.migrateFromCsv(CSV);

        assertEquals(1, result.conflicts());
        assertEquals("DN-9999", products.get("p1").getHolooCode(), "کدِ موجود دست نمی‌خورد");
        assertEquals(2, result.assigned());
    }

    @Test
    void محصولِ_نبودن_در_دیتابیس_گزارش_می‌شود_نه_خطا() {
        product("p1");   // p2 و p3 وجود ندارند

        var result = service.migrateFromCsv(CSV);

        assertEquals(2, result.missingProduct());
        assertEquals(1, result.assigned());
        assertEquals(2, result.missingDetail().size());
    }

    @Test
    void مهاجرت_مصرفِ_رزرو_را_گزارش_می‌کند() {
        HolooCode reserved = service.reserve("برای کالای الف");
        assertEquals("DN-0001", reserved.getCode());   // همان کدی که CSV هم دارد
        product("p1");
        product("p2");
        product("p3");

        var result = service.migrateFromCsv(CSV);

        assertEquals(1, result.reservedConsumed(), "مصرفِ رزرو در مهاجرت نباید بی‌صدا باشد");
        assertTrue(result.reservedDetail().get(0).contains("برای کالای الف"),
                "یادداشتِ رزرو باید در شرح بیاید: " + result.reservedDetail());
    }

    @Test
    void پیشوند_از_خودِ_دادهٔ_مهاجرت_یاد_گرفته_می‌شود() {
        settings.setHolooCodePrefix(null);
        product("p1");
        product("p2");
        product("p3");

        service.migrateFromCsv(CSV);

        assertEquals("DN-", settings.getHolooCodePrefix(),
                "پیشوند باید در تنظیمات بنشیند، نه در کدِ هسته");
    }
}
