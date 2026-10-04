package org.example.shop1.model.service.holoo;

import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.ActivityLog;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.entity.StoreSettings;
import org.example.shop1.model.reposritory.ProductRepository;
import org.example.shop1.model.service.ActivityLogService;
import org.example.shop1.model.service.StockNotificationService;
import org.example.shop1.model.service.StoreSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * نگهبانِ ایمپورتِ موجودی — هر تست یک شکستِ بی‌صدا را می‌بندد.
 * <p>
 * چیزهایی که اگر بشکنند هیچ خطایی نمی‌دهند و فقط عددِ غلط منتشر می‌شود:
 * ردیفِ جمعِ کل که کالا حساب شود، فایلی که ستونِ کد ندارد و بی‌صدا صفر ردیف
 * وارد کند، ایمپورتِ دوباره‌ای که همه‌چیز را دوباره بنویسد و صدها پیامک بفرستد،
 * و ویرایشِ دستیِ انسان که بی‌سروصدا بازنویسی شود.
 */
class HolooStockImportTest {

    private ProductRepository productRepo;
    private StockNotificationService notifications;
    private HolooStockImportService service;
    private StoreSettings settings;

    private final Map<String, Product> db = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        productRepo = mock(ProductRepository.class);
        notifications = mock(StockNotificationService.class);
        StoreSettingsService settingsService = mock(StoreSettingsService.class);
        settings = new StoreSettings();
        when(settingsService.getSettings()).thenReturn(settings);

        when(productRepo.findAll()).thenAnswer(inv -> new ArrayList<>(db.values()));
        when(productRepo.findById(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(db.get(inv.getArgument(0, String.class))));
        when(productRepo.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            db.put(p.getId(), p);
            return p;
        });

        service = new HolooStockImportService(productRepo, settingsService, notifications,
                mock(ActivityLogService.class));
    }

    // ==========================================================

    private Product product(String id, String code, Integer isfahan, Integer tehran) {
        Product p = new Product();
        p.setId(id);
        p.setName("محصول " + id);
        p.setHolooCode(code);
        p.setStockIsfahan(isfahan);
        p.setStockTehran(tehran);
        p.setStock((isfahan == null ? 0 : isfahan) + (tehran == null ? 0 : tehran));
        db.put(id, p);
        return p;
    }

    /** ردیفِ داده با همان پنج ستونِ خروجیِ واقعی، به‌علاوهٔ ستونِ کد که شرکت اضافه می‌کند. */
    private static List<Object> row(Object index, String name, String warehouse, String group,
                                    Object quantity, String code) {
        return java.util.Arrays.asList(index, name, warehouse, group, quantity, code);
    }

    private static List<Object> header() {
        // ⚠️ سرستونِ ستونِ کد عمداً «کد فنی» است، نه چیزی که کد انتظارش را داشته باشد:
        // نامِ سرستون هاردکد نیست و تشخیص باید از روی الگویِ مقادیر انجام شود.
        return java.util.Arrays.asList("رديف", "نام", "گروه اصلي", "گروه فرعي", "موجودي", "کد فني");
    }

    private HolooStockImportService.UploadedFile file(String name, List<List<Object>> rows) throws Exception {
        byte[] bytes = XlsxTestWorkbook.build(List.of(new XlsxTestWorkbook.SheetSpec("Sheet1", rows)));
        return new HolooStockImportService.UploadedFile(name, bytes);
    }

    // ==========================================================

    @Test
    void ردیفِ_جمعِ_کل_کالا_حساب_نمی‌شود() throws Exception {
        product("p1", "DN-0001", 0, 0);

        // بازسازیِ دقیقِ دنبالهٔ فایلِ واقعی: دو کالا و بعد ردیفِ جمعِ کل
        List<List<Object>> rows = List.of(
                header(),
                row(266, "hEX S", "انبار اصفهان", "شبکه", 6, "DN-0001"),
                row(267, "server ", "انبار اصفهان", "شبکه", 1, null),
                row(null, null, null, null, 180101, null));

        var preview = service.preview(List.of(file("اصفهان.xlsx", rows)));

        assertEquals(1, preview.rowsSkipped(), "ردیفِ جمعِ کل باید رد شود");
        assertEquals(2, preview.rowsRead());
        assertTrue(preview.changes().stream().noneMatch(c -> c.newStock() == 180101),
                "عددِ جمعِ کل نباید به هیچ محصولی نشسته باشد");
    }

    @Test
    void فایلِ_بی‌کد_خطای_روشن_می‌دهد_نه_صفر_ردیف() throws Exception {
        product("p1", "DN-0001", 5, 0);

        List<List<Object>> rows = List.of(
                java.util.Arrays.asList("رديف", "نام", "گروه اصلي", "گروه فرعي", "موجودي"),
                java.util.Arrays.asList(1, "hEX S", "انبار اصفهان", "شبکه", 6));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.preview(List.of(file("بی‌کد.xlsx", rows))));
        assertTrue(ex.getMessage().contains("کدِ کالا"), ex.getMessage());
    }

    @Test
    void انبار_از_داده_خوانده_می‌شود_نه_از_نامِ_فایل() throws Exception {
        product("p1", "DN-0001", 0, 0);

        // نامِ فایل می‌گوید اصفهان، ولی دادهٔ ستونِ گروه اصلي می‌گوید تهران. داده برنده است.
        List<List<Object>> rows = List.of(
                header(),
                row(1, "hEX S", "انبار تهران", "شبکه", 7, "DN-0001"));

        var preview = service.preview(List.of(file("انبار-اصفهان-۶-شهریور.xlsx", rows)));

        assertEquals(1, preview.changes().size());
        var change = preview.changes().get(0);
        assertEquals(7, change.newTehran(), "عدد باید روی تهران بنشیند");
        assertEquals(0, change.newIsfahan());
    }

    @Test
    void نبودن_در_فایل_یعنی_صفرِ_همان_انبار_و_انبارِ_دیگر_دست‌نخورده() throws Exception {
        product("p1", "DN-0001", 5, 3);   // در فایل نیست
        product("p2", "DN-0002", 0, 0);

        List<List<Object>> rows = List.of(
                header(),
                row(1, "دومی", "انبار اصفهان", "شبکه", 4, "DN-0002"));

        var preview = service.preview(List.of(file("اصفهان.xlsx", rows)));

        var missing = preview.changes().stream()
                .filter(c -> "p1".equals(c.productId())).findFirst().orElseThrow();
        assertEquals(0, missing.newIsfahan(), "انبارِ پوشش‌داده‌شده صفر می‌شود");
        assertEquals(3, missing.newTehran(), "انبارِ پوشش‌نداده‌شده دست نمی‌خورد");
        assertEquals(3, missing.newStock(), "stock = جمعِ دو انبار");
    }

    /**
     * 🔴 کالایی که فیلدِ انبارش خالی است ولی {@code stock}ِ میراثی دارد.
     * <p>
     * در اولین ایمپورتِ واقعی ۵۷ کالا دقیقاً همین شکل بودند و از تور رد شدند:
     * {@code eq(null, 0)} درست است، پس هیچ تغییری ساخته نمی‌شد و {@code stock} روی
     * ۹۹۹ می‌ماند — یعنی روی سایت «موجود» می‌ماندند در حالی که در فایلِ انبار نبودند.
     */
    @Test
    void انبارِ_خالی_با_stockِ_میراثی_از_تور_رد_نمی‌شود() throws Exception {
        Product ghost = product("p-ghost", "DN-0002", null, null);
        ghost.setStock(999);                       // میراثِ وارداتِ قدیمی
        product("p1", "DN-0001", 0, 0);

        List<List<Object>> rows = List.of(
                header(),
                row(1, "کالای واقعی", "انبار اصفهان", "شبکه", 4, "DN-0001"));

        var preview = service.preview(List.of(file("اصفهان.xlsx", rows)));

        var change = preview.changes().stream()
                .filter(c -> "p-ghost".equals(c.productId())).findFirst()
                .orElseThrow(() -> new AssertionError("کالای بی‌انبار باید در پیش‌نمایش دیده شود"));
        assertEquals(999, change.oldStock());
        assertEquals(0, change.newStock());
        assertTrue(change.zeroing(), "باید در شمارشِ «صفر می‌شود» بیاید");
        assertFalse(change.wouldSms(), "گذارِ مثبت→صفر است، پس نباید پیامک بسازد");

        // تیکِ پیامک عمداً روشن است تا اثبات شود سکوتِ این ردیف از خودِ گذارش می‌آید،
        // نه از خاموش‌بودنِ تیک. (کالای دیگرِ صحنه واقعاً صفر→مثبت می‌شود و پیامکش درست است.)
        service.apply(preview.token(), true, true);
        assertEquals(0, db.get("p-ghost").getStock(), "stock باید واقعاً صفر شود");
        verify(notifications, never()).notifyBackInStock(eq("p-ghost"), anyString());
    }

    /**
     * یک ایمپورت فقط <b>یک</b> رکوردِ خلاصه می‌سازد.
     * <p>
     * پیش از این هر محصولِ عوض‌شده یک {@code STOCK_CHANGE} هم می‌گرفت؛ یک ایمپورتِ
     * ۸۷تایی تاریخچه را می‌پوشاند و ویرایش‌های دستی — که واقعاً باید دیده شوند —
     * زیرش گم می‌شدند.
     */
    @Test
    void ایمپورت_فقط_یک_رکوردِ_خلاصه_می‌سازد() throws Exception {
        ActivityLogService log = mock(ActivityLogService.class);
        StoreSettingsService settingsService = mock(StoreSettingsService.class);
        when(settingsService.getSettings()).thenReturn(settings);
        HolooStockImportService svc = new HolooStockImportService(
                productRepo, settingsService, notifications, log);

        List<List<Object>> rows = new ArrayList<>();
        rows.add(header());
        for (int i = 1; i <= 4; i++) {
            String code = String.format("DN-%04d", i);
            product("p" + i, code, 0, 0);
            rows.add(row(i, "کالا " + i, "انبار اصفهان", "شبکه", i, code));
        }

        var preview = svc.preview(List.of(file("اصفهان.xlsx", rows)));
        var result = svc.apply(preview.token(), false, false);

        assertEquals(4, result.changed());
        verify(log, never()).recordProduct(eq(ActivityLog.Action.STOCK_CHANGE),
                eq(ActivityLog.Source.HOLOO), anyString(), anyString(), anyString(), any(), any());
        verify(log, times(1)).record(eq(ActivityLog.Action.HOLOO_STOCK_IMPORT),
                eq(ActivityLog.Source.HOLOO), anyString(), any(), anyString(), anyString(), any(), any());
    }

    /** رگرسیونِ بی‌اثر‌پذیری: کالایی که عددش با فایل می‌خواند نباید تغییر بگیرد. */
    @Test
    void کالایی_که_عددش_با_فایل_می‌خواند_تغییر_نمی‌گیرد() throws Exception {
        product("p1", "DN-0001", 4, 0);

        List<List<Object>> rows = List.of(
                header(),
                row(1, "کالا", "انبار اصفهان", "شبکه", 4, "DN-0001"));

        var preview = service.preview(List.of(file("اصفهان.xlsx", rows)));
        assertTrue(preview.changes().isEmpty(), "عددِ یکسان نباید تغییری بسازد");
    }

    @Test
    void ایمپورتِ_دوباره_هیچ_تغییری_و_هیچ_پیامکی_ندارد() throws Exception {
        product("p1", "DN-0001", 0, 0);

        List<List<Object>> rows = List.of(
                header(),
                row(1, "hEX S", "انبار اصفهان", "شبکه", 6, "DN-0001"));

        var first = service.preview(List.of(file("اصفهان.xlsx", rows)));
        assertEquals(1, first.changes().size());
        assertEquals(1, first.smsCandidates(), "گذارِ صفر → مثبت یک نامزدِ پیامک است");
        service.apply(first.token(), false, false);

        verifyNoInteractions(notifications);   // تیکِ پیامک خاموش بود

        var second = service.preview(List.of(file("اصفهان.xlsx", rows)));
        assertTrue(second.changes().isEmpty(), "اجرای دوباره نباید چیزی عوض کند");
        assertEquals(0, second.smsCandidates(), "و نباید هیچ پیامکی نامزد شود");

        var result = service.apply(second.token(), true, false);
        assertEquals(0, result.changed());
        assertEquals(0, result.smsSent());
        verifyNoInteractions(notifications);
    }

    @Test
    void پیامک_فقط_با_تیکِ_صریح_می‌رود() throws Exception {
        product("p1", "DN-0001", 0, 0);
        List<List<Object>> rows = List.of(header(), row(1, "hEX S", "انبار اصفهان", "شبکه", 6, "DN-0001"));

        var preview = service.preview(List.of(file("اصفهان.xlsx", rows)));
        var result = service.apply(preview.token(), true, false);

        assertEquals(1, result.smsSent());
        verify(notifications).notifyBackInStock("p1", "محصول p1");
    }

    @Test
    void سقفِ_پیامک_ایمپورت_را_متوقف_می‌کند() throws Exception {
        settings.setStockImportSmsCap(2);
        List<List<Object>> rows = new ArrayList<>();
        rows.add(header());
        for (int i = 1; i <= 5; i++) {
            String code = String.format("DN-%04d", i);
            product("p" + i, code, 0, 0);
            rows.add(row(i, "کالا " + i, "انبار اصفهان", "شبکه", 3, code));
        }

        var preview = service.preview(List.of(file("اصفهان.xlsx", rows)));
        assertEquals(5, preview.smsCandidates());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.apply(preview.token(), true, false));
        assertTrue(ex.getMessage().contains("سقف"), ex.getMessage());

        // با تیکِ خاموش همان ایمپورت بی‌مشکل اعمال می‌شود
        var ok = service.apply(preview.token(), false, false);
        assertEquals(5, ok.changed());
        assertEquals(0, ok.smsSent());
    }

    @Test
    void گاردِ_صفرشدنِ_انبوه_بدونِ_تأیید_رد_می‌کند() throws Exception {
        settings.setStockImportMassZeroPercent(30);
        for (int i = 1; i <= 10; i++) {
            product("p" + i, String.format("DN-%04d", i), 5, 0);
        }

        // فایلِ عمداً ناقص: فقط یک کالا از ده تا
        List<List<Object>> rows = List.of(
                header(),
                row(1, "کالا ۱", "انبار اصفهان", "شبکه", 5, "DN-0001"));

        var preview = service.preview(List.of(file("ناقص.xlsx", rows)));
        assertEquals(9, preview.zeroedCount());
        assertEquals(90, preview.zeroedPercent());
        assertTrue(preview.massZeroTripped());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.apply(preview.token(), false, false));
        assertTrue(ex.getMessage().contains("90"), ex.getMessage());

        var forced = service.apply(preview.token(), false, true);
        assertEquals(9, forced.changed());
    }

    @Test
    void بازنویسیِ_ویرایشِ_دستی_جدا_علامت_می‌خورد() throws Exception {
        Product manual = product("p1", "DN-0001", 5, 0);
        manual.setStockImportedAt(Instant.now().minusSeconds(3600));
        manual.setStockTouchedManuallyAt(Instant.now());        // «فروختم» بعد از ایمپورتِ قبلی

        Product untouched = product("p2", "DN-0002", 5, 0);
        untouched.setStockImportedAt(Instant.now().minusSeconds(3600));

        List<List<Object>> rows = List.of(
                header(),
                row(1, "کالا ۱", "انبار اصفهان", "شبکه", 9, "DN-0001"),
                row(2, "کالا ۲", "انبار اصفهان", "شبکه", 9, "DN-0002"));

        var preview = service.preview(List.of(file("اصفهان.xlsx", rows)));

        assertEquals(1, preview.manualOverwrites().size());
        assertEquals("p1", preview.manualOverwrites().get(0).productId());
        assertEquals(2, preview.changes().size(), "هر دو عوض می‌شوند، ولی فقط یکی برجسته است");
    }

    @Test
    void ردیفِ_تطبیق‌نخورده_گزارش_می‌شود_نه_دور_ریخته() throws Exception {
        product("p1", "DN-0001", 0, 0);

        List<List<Object>> rows = List.of(
                header(),
                row(1, "hEX S", "انبار اصفهان", "شبکه", 6, "DN-0001"),
                row(2, "WS-C2960 سیسکو", "انبار اصفهان", "سیسکو", 3, "DN-9999"),
                row(3, "کالای بی‌کد", "انبار اصفهان", "شبکه", 2, null));

        var preview = service.preview(List.of(file("اصفهان.xlsx", rows)));

        assertEquals(2, preview.unmatched().size());
        assertTrue(preview.unmatched().stream()
                .anyMatch(u -> "DN-9999".equals(u.code()) && u.reason().contains("هیچ محصولی")));
        assertTrue(preview.unmatched().stream()
                .anyMatch(u -> u.code() == null && u.reason().contains("کدِ کالا ندارد")));
    }

    @Test
    void دو_فایل_با_هم_هر_دو_انبار_را_پوشش_می‌دهند() throws Exception {
        product("p1", "DN-0001", 5, 5);

        var isfahan = file("اصفهان.xlsx", List.of(header(),
                row(1, "کالا", "انبار اصفهان", "شبکه", 2, "DN-0001")));
        var tehran = file("تهران.xlsx", List.of(header(),
                row(1, "کالا", "انبار تهران", "شبکه", 3, "DN-0001")));

        var preview = service.preview(List.of(isfahan, tehran));

        assertEquals(1, preview.changes().size());
        var c = preview.changes().get(0);
        assertEquals(2, c.newIsfahan());
        assertEquals(3, c.newTehran());
        assertEquals(5, c.newStock());
        assertEquals(2, preview.coveredBranches().size());
    }

    @Test
    void تغییرِ_وضعیتِ_ترب_شمرده_می‌شود() throws Exception {
        Product p = product("p1", "DN-0001", 4, 0);
        p.setTorobEnabled(true);

        List<List<Object>> rows = List.of(header(),
                row(1, "کالا", "انبار اصفهان", "شبکه", 0, "DN-0001"));

        var preview = service.preview(List.of(file("اصفهان.xlsx", rows)));

        assertEquals(1, preview.torobFlips(), "موجود → ناموجود یعنی availabilityِ ترب عوض می‌شود");
    }
}
