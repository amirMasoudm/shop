package org.example.shop1.model.service;

import org.example.shop1.model.entity.LegacyRedirect;
import org.example.shop1.model.reposritory.LegacyRedirectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * نگهبانِ صاف‌کردنِ زنجیره و حذفِ حلقه هنگامِ ثبتِ تکیِ ریدایرکت.
 * <p>
 * 🔴 چرا لازم است: {@code LegacyRedirectService.resolve} فقط یک
 * {@code findByFromPath} می‌زند و <b>هیچ پرشی را دنبال نمی‌کند</b> — برخلافِ
 * {@code ProductRedirectService} که {@code MAX_HOPS} و {@code visited} دارد. پس
 * زنجیره باید در <b>زمانِ نوشتن</b> صاف شود، وگرنه {@code A → B → C} یعنی
 * {@code A} به صفحه‌ای می‌رسد که خودش ۳۰۱ می‌دهد. شکستنش هیچ خطایی نمی‌دهد؛ فقط
 * یک پرشِ اضافه به گوگل نشان می‌دهد.
 * <p>
 * مسیرهای این تست عمداً <b>خارج از پیشوندهای رزرو</b> انتخاب شده‌اند. دلیلش را در
 * آخرین تستِ همین کلاس ببین.
 */
class LegacyRedirectChainTest {

    private LegacyRedirectRepository repo;
    private LegacyRedirectService service;

    /** کلید: {@code fromPath} — همان یکتاییِ واقعیِ کالکشن. */
    private Map<String, LegacyRedirect> store;

    /** در این تست‌ها مقصد همیشه هست؛ نبودنش تستِ جداگانه دارد. */
    private static final Predicate<String> TARGET_EXISTS = p -> true;

    @BeforeEach
    void setUp() {
        store = new LinkedHashMap<>();
        repo = mock(LegacyRedirectRepository.class);

        when(repo.findByFromPath(anyString()))
                .thenAnswer(i -> Optional.ofNullable(store.get(i.getArgument(0, String.class))));
        when(repo.findAll()).thenAnswer(i -> new ArrayList<>(store.values()));
        when(repo.count()).thenAnswer(i -> (long) store.size());
        when(repo.save(any(LegacyRedirect.class))).thenAnswer(i -> {
            LegacyRedirect r = i.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID().toString());
            store.entrySet().removeIf(e -> e.getValue().getId().equals(r.getId()));
            store.put(r.getFromPath(), r);
            return r;
        });
        doAnswer(i -> {
            LegacyRedirect r = i.getArgument(0);
            store.remove(r.getFromPath());
            return null;
        }).when(repo).delete(any(LegacyRedirect.class));

        service = new LegacyRedirectService(repo, null);
    }

    private void seed(String from, String to) {
        LegacyRedirect r = new LegacyRedirect();
        r.setId(UUID.randomUUID().toString());
        r.setFromPath(LegacyRedirectService.normalize(from));
        r.setToPath(to);
        store.put(r.getFromPath(), r);
    }

    private String targetOf(String from) {
        LegacyRedirect r = store.get(LegacyRedirectService.normalize(from));
        return r == null ? null : r.getToPath();
    }

    @Test
    void زنجیره_صاف_می‌شود_نه_دوپرشی() {
        seed("/راه-اندازی-میکروتیک", "/آموزش-میکروتیک");

        Map<String, Object> out = service.addOneFlattened(
                "/آموزش-میکروتیک", "/میکروتیک-از-صفر", "تغییرِ اسلاگ", TARGET_EXISTS);

        assertEquals(1, out.get("flattened"), "ریدایرکتِ قدیمی باید مستقیم شده باشد");
        assertEquals("/میکروتیک-از-صفر", targetOf("/آموزش-میکروتیک"));
        // 🔴 اصلِ مطلب: مسیرِ اولی دیگر به مسیرِ میانی نمی‌رسد، مستقیم به مقصدِ تازه می‌رود
        assertEquals("/میکروتیک-از-صفر", targetOf("/راه-اندازی-میکروتیک"));
    }

    @Test
    void برگرداندنِ_اسلاگ_حلقه_نمی‌سازد() {
        seed("/اسلاگ-قدیم", "/اسلاگ-تازه");

        // کاربر پشیمان شد و اسلاگ را به مقدارِ قبلی برگرداند
        Map<String, Object> out = service.addOneFlattened(
                "/اسلاگ-تازه", "/اسلاگ-قدیم", null, TARGET_EXISTS);

        assertEquals(1, out.get("loopsRemoved"));
        assertNull(targetOf("/اسلاگ-قدیم"), "ریدایرکتِ قبلی باید حذف شده باشد");
        assertEquals("/اسلاگ-قدیم", targetOf("/اسلاگ-تازه"));
        assertEquals(1, store.size(), "فقط یک رکورد باید بماند، نه دو رکوردِ روبه‌هم");
    }

    @Test
    void مسیرِ_قدیم_و_تازهٔ_یکسان_ثبت_نمی‌شود() {
        Map<String, Object> out = service.addOneFlattened(
                "/همان-اسلاگ", "/همان-اسلاگ", null, TARGET_EXISTS);

        assertEquals(Boolean.TRUE, out.get("unchanged"));
        assertTrue(store.isEmpty(), "هیچ رکوردِ خودارجاعی نباید ساخته شود");
    }

    @Test
    void مقصدِ_ناموجود_زنجیرهٔ_سالم_را_خراب_نمی‌کند() {
        seed("/اول", "/دوم");

        Map<String, Object> out = service.addOneFlattened("/دوم", "/سوم", null, p -> false);

        assertEquals(0, out.get("flattened"));
        // صاف‌کردن به مقصدی که پذیرفته نشده، از زنجیرهٔ دوپرشی بدتر است
        assertEquals("/دوم", targetOf("/اول"));
    }

    @Test
    void مقایسه_به_درصد_کدشدن_حساس_نیست() {
        // همان «/اول»، این‌بار درصد-کدشده — نرمال‌سازی باید یکی‌شان بداند
        seed("/%D8%A7%D9%88%D9%84", "/دوم");

        Map<String, Object> out = service.addOneFlattened("/دوم", "/سوم", null, TARGET_EXISTS);

        assertEquals(1, out.get("flattened"));
        assertEquals("/سوم", targetOf("/اول"));
    }

    /**
     * 🔴 <b>قفلی که تسکِ ریدایرکتِ اسلاگِ مقاله به آن خورد.</b>
     * <p>
     * {@code /blog} جزو پیشوندهای رزرو است، پس هیچ مسیرِ {@code /blog/…} نمی‌تواند
     * <b>مبدأِ</b> ریدایرکت شود. علتش عمدی است: {@code LegacyRedirectFilter} پیش از
     * مسیریابیِ اسپرینگ اجرا می‌شود، پس یک ریدایرکتِ {@code /blog/x} می‌توانست مقالهٔ
     * زندهٔ همان اسلاگ را بدزدد.
     * <p>
     * این تست عمداً وضعِ <i>موجود</i> را تثبیت می‌کند، نه وضعِ مطلوب را. اگر روزی
     * تصمیم گرفته شد مقاله هم مثلِ محصول ریدایرکتِ اسلاگ بگیرد، این تست باید
     * آگاهانه عوض شود — نه اینکه بی‌صدا رد شود.
     */
    @Test
    void مسیرِ_بلاگ_فعلاً_مبدأِ_ریدایرکت_نمی‌شود() {
        assertTrue(LegacyRedirectService.isReservedPath("/blog/هر-اسلاگی"));

        Map<String, Object> out = service.addOneFlattened(
                "/blog/اسلاگ-قدیم", "/blog/اسلاگ-تازه", null, TARGET_EXISTS);

        assertEquals(0, (int) out.get("created") + (int) out.get("updated"));
        assertTrue(store.isEmpty());
    }
}
