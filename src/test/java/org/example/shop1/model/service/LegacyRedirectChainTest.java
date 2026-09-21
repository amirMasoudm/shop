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
     * 🔴 <b>استثنای «دیرحل» — این تست عمداً عوض شد.</b>
     * <p>
     * پیش از این، {@code /blog} جزو پیشوندهای رزرو بود و هیچ مسیرِ {@code /blog/…}
     * نمی‌توانست مبدأِ ریدایرکت شود. علتش این بود که {@code LegacyRedirectFilter}
     * پیش از مسیریابیِ اسپرینگ اجرا می‌شود و یک رکوردِ کهنه می‌توانست مقالهٔ زندهٔ
     * همان اسلاگ را بدزدد.
     * <p>
     * حالا همان نگهبان به شکلِ دقیق‌تری برقرار است: فیلتر عمداً به این مسیرها دست
     * نمی‌زند و ریدایرکت فقط در کنترلر و فقط <b>بعد از شکستِ</b> پیداکردنِ مقاله
     * اعمال می‌شود. پس مقالهٔ زنده همچنان مقدم است، ولی اسلاگِ مرده می‌تواند ۳۰۱ بدهد.
     */
    @Test
    void زیرمسیرِ_بلاگ_مبدأ_می‌شود_ولی_خودِ_بلاگ_و_هاب_نه() {
        assertFalse(LegacyRedirectService.isReservedPath("/blog/یک-اسلاگ"),
                "اسلاگِ مقاله باید بتواند مبدأ باشد");
        assertTrue(LegacyRedirectService.isLateResolved("/blog/یک-اسلاگ"));

        // خودِ فهرستِ بلاگ و مسیرِ هاب هندلرِ فروافتادن ندارند، پس همچنان رزرو می‌مانند
        assertTrue(LegacyRedirectService.isReservedPath("/blog"));
        assertTrue(LegacyRedirectService.isReservedPath("/blog/hub/میکروتیک"));
        assertFalse(LegacyRedirectService.isLateResolved("/blog/hub/میکروتیک"));

        Map<String, Object> out = service.addOneFlattened(
                "/blog/اسلاگ-قدیم", "/blog/اسلاگ-تازه", null, TARGET_EXISTS);

        assertEquals(1, (int) out.get("created") + (int) out.get("updated"));
        assertEquals("/blog/اسلاگ-تازه", targetOf("/blog/اسلاگ-قدیم"));
    }

    /**
     * 🔴 این را تستِ دستی روی دادهٔ واقعی گرفت، نه کدخوانی: مسیرِ وردپرسی بعد از
     * چند تغییرِ اسلاگ «۳۰۱ به ۴۰۴» می‌داد.
     * <p>
     * ⚠️ <b>مرحلهٔ میانی حذف‌شدنی نیست.</b> نسخهٔ اولِ همین تست سناریو را
     * الف→ب→الف نوشته بود و روی کدِ <i>پیش از</i> اصلاح هم پاس می‌شد، چون آن‌جا
     * مسیرِ قدیم دقیقاً همان مقصدی است که رکوردِ وردپرسی به آن اشاره دارد و قاعدهٔ
     * عادیِ صاف‌کردن ({@code toPath == from}) خودش می‌گیردش.
     * <p>
     * چیزی که واقعاً اتفاق افتاد یک مرحلهٔ میانی داشت که در آن کاربر روی «خیر»
     * زد، پس <b>هیچ ریدایرکتی ثبت نشد</b> — اسلاگ از ب به ج رفت بی‌آنکه سرویس
     * خبردار شود. بعد که اسلاگ به الف برگشت، مسیرِ قدیم «ج» بود و رکوردِ وردپرسی
     * هنوز به «ب» اشاره می‌کرد؛ حلقه‌زدایی «ب» را رها کرد و قاعدهٔ عادی آن را
     * نمی‌دید. نتیجه: آدرسِ ایندکس‌شده‌ای که سالم بود، ۴۰۴ شد.
     */
    @Test
    void رهاشدنِ_اسلاگِ_میانی_ریدایرکتِ_قدیمی_را_به_۴۰۴_نمی‌فرستد() {
        seed("/وردپرس-قدیمی", "/blog/الف");

        // مرحلهٔ ۱ — تغییرِ اسلاگ با «بله»: رکوردِ وردپرسی به «ب» صاف می‌شود
        service.addOneFlattened("/blog/الف", "/blog/ب", null, TARGET_EXISTS);
        assertEquals("/blog/ب", targetOf("/وردپرس-قدیمی"), "پیش‌شرط: صاف شده باشد");

        // مرحلهٔ ۲ — تغییرِ اسلاگ از ب به ج با «خیر»: عمداً هیچ فراخوانی‌ای نیست.
        // همین‌جاست که «ب» از دستِ قاعدهٔ عادی در می‌رود.

        // مرحلهٔ ۳ — برگشت به الف
        service.addOneFlattened("/blog/ج", "/blog/الف", null, TARGET_EXISTS);

        assertNull(targetOf("/blog/الف"), "حلقه باید حذف شده باشد");
        assertEquals("/blog/الف", targetOf("/blog/ج"));
        // 🔴 اصلِ مطلب: وردپرس دیگر به «ب» که حالا مرده است اشاره نمی‌کند
        assertEquals("/blog/الف", targetOf("/وردپرس-قدیمی"),
                "رکوردِ وردپرسی باید از اسلاگِ رهاشده به مقصدِ زنده منتقل شده باشد");
    }

    /**
     * سناریویِ واقعیِ تلهٔ سوم: یکی از ریدایرکت‌های وردپرسی مقصدش مقاله‌ای است که
     * اسلاگش عوض می‌شود. بعد از تغییر باید <b>مستقیم</b> به اسلاگِ تازه برسد، نه با
     * دو پرش.
     */
    @Test
    void ریدایرکتِ_وردپرسی_بعد_از_تغییرِ_اسلاگِ_مقصد_مستقیم_می‌شود() {
        seed("/عیبیابی-میکروتیک-به-روش-متخصصان", "/blog/عیبیابی-میکروتیک");

        Map<String, Object> out = service.addOneFlattened(
                "/blog/عیبیابی-میکروتیک", "/blog/عیب-یابی-میکروتیک", null, TARGET_EXISTS);

        assertEquals(1, out.get("flattened"));
        assertEquals("/blog/عیب-یابی-میکروتیک", targetOf("/عیبیابی-میکروتیک-به-روش-متخصصان"));
    }
}
