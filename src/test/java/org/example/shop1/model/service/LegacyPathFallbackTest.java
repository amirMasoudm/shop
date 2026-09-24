package org.example.shop1.model.service;

import jakarta.servlet.RequestDispatcher;
import org.example.shop1.model.entity.Article;
import org.example.shop1.model.entity.LegacyRedirect;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.reposritory.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * نگهبانِ فالبکِ ۴۰۴.
 * <p>
 * این فالبک روی هر درخواستی که به ۴۰۴ می‌رسد اجرا می‌شود، پس دو خطا اینجا گران است:
 * <b>کم‌گرفتن</b> یعنی صدها آدرسِ ایندکس‌شده می‌افتند، و <b>زیاد‌گرفتن</b> یعنی آدرسِ
 * بی‌معنا یا مسیرِ APIِ محافظت‌شده به جایی ریدایرکت می‌شود. تست‌ها هر دو طرف را
 * می‌پوشانند.
 */
class LegacyPathFallbackTest {

    private ArticleService articleService;
    private ProductRepository productRepo;
    private LegacyRedirectService legacyRedirects;
    private LegacyPathFallback fallback;

    @BeforeEach
    void setUp() {
        articleService = mock(ArticleService.class);
        productRepo = mock(ProductRepository.class);
        legacyRedirects = mock(LegacyRedirectService.class);

        when(articleService.getPublishedBySlugOrId(anyString())).thenReturn(Optional.empty());
        when(productRepo.findBySlug(anyString())).thenReturn(Optional.empty());
        when(legacyRedirects.resolve(anyString())).thenReturn(Optional.empty());

        // جدولِ مسیریابیِ ساختگی: فقط /shop و /blog هندلر دارند.
        // ⚠️ PathPatternParser صریح لازم است: بدونِ آن RequestMappingInfo شرطِ الگو را
        // به شکلِ قدیمیِ AntPath می‌سازد و getPathPatternsCondition نال برمی‌گرداند —
        // درحالی‌که اپِ واقعی روی اسپرینگ‌بوت ۳ همین پارسر را دارد.
        RequestMappingInfo.BuilderConfiguration config = new RequestMappingInfo.BuilderConfiguration();
        config.setPatternParser(new org.springframework.web.util.pattern.PathPatternParser());
        RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, org.springframework.web.method.HandlerMethod> handlers = new HashMap<>();
        handlers.put(RequestMappingInfo.paths("/shop").methods(RequestMethod.GET).options(config).build(), null);
        handlers.put(RequestMappingInfo.paths("/blog").methods(RequestMethod.GET).options(config).build(), null);
        // 🔴 الگوها عمداً اینجا هستند: حلقهٔ ۳۰۱ فقط با آن‌ها بازتولید می‌شود، چون
        // hasGetHandler با الگو تطبیق می‌دهد نه با وجودِ واقعیِ مقاله/محصول.
        handlers.put(RequestMappingInfo.paths("/blog/{slugOrId}").methods(RequestMethod.GET).options(config).build(), null);
        handlers.put(RequestMappingInfo.paths("/blog/hub/{slug}").methods(RequestMethod.GET).options(config).build(), null);
        handlers.put(RequestMappingInfo.paths("/shop/product/{slug}").methods(RequestMethod.GET).options(config).build(), null);
        handlers.put(RequestMappingInfo.paths("/shop/category/{slug}").methods(RequestMethod.GET).options(config).build(), null);
        when(mapping.getHandlerMethods()).thenReturn(handlers);

        ObjectProvider<RequestMappingHandlerMapping> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mapping);

        fallback = new LegacyPathFallback(articleService, productRepo, legacyRedirects, provider);
    }

    /** درخواستی که به ۴۰۴ رسیده — همان چیزی که کنترلرِ خطا می‌بیند. */
    private MockHttpServletRequest notFound(String uri) {
        MockHttpServletRequest r = new MockHttpServletRequest("GET", "/error");
        r.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, uri);
        r.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        return r;
    }

    private void publishedArticle(String slug) {
        Article a = new Article();
        a.setSlug(slug);
        when(articleService.getPublishedBySlugOrId(slug)).thenReturn(Optional.of(a));
    }

    private void redirect(String from, String to) {
        LegacyRedirect r = new LegacyRedirect();
        r.setFromPath(from);
        r.setToPath(to);
        when(legacyRedirects.resolve(from)).thenReturn(Optional.of(r));
    }

    // ── قاعدهٔ ۱ ──────────────────────────────────────────────────────────
    @Test
    void اسلشِ_پایانی_به_مسیرِ_واقعی_می‌رسد() {
        assertEquals("/shop", fallback.resolve(notFound("/shop/")));
        assertEquals("/blog", fallback.resolve(notFound("/blog/")));
    }

    // ── قاعدهٔ ۲ ──────────────────────────────────────────────────────────
    @Test
    void مسیرِ_تک‌بخشی_که_اسلاگِ_مقاله_است() {
        publishedArticle("keyloggers");
        assertEquals("/blog/keyloggers", fallback.resolve(notFound("/keyloggers/")));
        assertEquals("/blog/keyloggers", fallback.resolve(notFound("/keyloggers")));
    }

    @Test
    void اسلاگِ_فارسیِ_درصد_کدشده_هم_کار_می‌کند() {
        publishedArticle("روش-کدگذاری-هافمن");
        assertEquals("/blog/روش-کدگذاری-هافمن",
                fallback.resolve(notFound("/%D8%B1%D9%88%D8%B4-%DA%A9%D8%AF%DA%AF%D8%B0%D8%A7%D8%B1%DB%8C-%D9%87%D8%A7%D9%81%D9%85%D9%86/")));
    }

    /**
     * 🔴 مهم‌ترین تستِ این کلاس: اسلاگِ مقاله عوض شده، پس {@code /blog/{x}} خودش ۳۰۱
     * می‌دهد. رفتن به آن یعنی دو پرش؛ باید یک‌راست به مقصدِ نهایی رفت.
     */
    @Test
    void اسلاگِ_عوض‌شده_یک_پرش_تا_مقصدِ_نهایی() {
        redirect("/blog/mtctce-06-97", "/blog/دوره-mtctce-شهریور-97");
        assertEquals("/blog/دوره-mtctce-شهریور-97", fallback.resolve(notFound("/mtctce-06-97/")));
    }

    // ── قاعدهٔ ۲-ب: پرمالینکِ تاریخ‌دارِ وردپرس ─────────────────────────────
    /** لینک‌هایی که هنوز داخلِ متنِ مقالات مانده‌اند و تا امروز ۴۰۴ می‌دادند. */
    @Test
    void پرمالینکِ_تاریخ‌دار_به_مقالهٔ_زنده_می‌رسد() {
        publishedArticle("mikrotik-introduction");
        assertEquals("/blog/mikrotik-introduction",
                fallback.resolve(notFound("/2025/05/01/mikrotik-introduction/")), "چهاربخشی");
        assertEquals("/blog/mikrotik-introduction",
                fallback.resolve(notFound("/2025/05/mikrotik-introduction/")), "سه‌بخشی");
        assertEquals("/blog/mikrotik-introduction",
                fallback.resolve(notFound("/2025/05/01/mikrotik-introduction")), "بی‌اسلش");
    }

    /** دقیقاً مثلِ قاعدهٔ ۲: اسلاگِ عوض‌شده یک پرش تا مقصدِ نهایی، نه دو. */
    @Test
    void پرمالینکِ_تاریخ‌دار_با_اسلاگِ_عوض‌شده_یک_پرش() {
        redirect("/blog/old-slug", "/blog/new-slug");
        assertEquals("/blog/new-slug", fallback.resolve(notFound("/2025/05/01/old-slug/")));
    }

    /** نیمی از این لینک‌ها مقالهٔ متناظر ندارند؛ آن‌ها باید ۴۰۴ِ صادق بگیرند. */
    @Test
    void پرمالینکِ_تاریخ‌دارِ_بی‌مقاله_۴۰۴_می‌ماند() {
        assertNull(fallback.resolve(notFound("/2025/05/01/mikrotik-access-points/")));
    }

    /**
     * ⚠️ تاریخ باید معنادار باشد، نه فقط عدد. تستِ واقعی این است که <b>حتی با مقالهٔ
     * موجود</b> هم تاریخِ بی‌معنی رد شود — وگرنه «ماهِ ۱۳» فقط به‌خاطرِ نبودنِ مقاله
     * ۴۰۴ می‌گرفت و قاعده عملاً هر مسیرِ چهاربخشی را می‌پذیرفت.
     */
    @Test
    void تاریخِ_بی‌معنی_حتی_با_مقالهٔ_موجود_پذیرفته_نمی‌شود() {
        publishedArticle("mikrotik-introduction");
        assertNull(fallback.resolve(notFound("/2025/13/01/mikrotik-introduction/")), "ماهِ ۱۳");
        assertNull(fallback.resolve(notFound("/2025/05/32/mikrotik-introduction/")), "روزِ ۳۲");
        assertNull(fallback.resolve(notFound("/25/05/01/mikrotik-introduction/")), "سالِ دورقمی");
        assertNull(fallback.resolve(notFound("/abcd/05/01/mikrotik-introduction/")), "سالِ غیرعددی");
        assertNull(fallback.resolve(notFound("/shop/05/01/mikrotik-introduction/")), "پیشوندِ غیرتاریخی");
        assertNull(fallback.resolve(notFound("/2025/13/99/chizi-ke-nist/")), "نمونهٔ خودِ تسک");
    }

    /** {@code /2025/05/01} بایگانیِ روز است، نه نوشته‌ای به اسلاگِ «01». */
    @Test
    void بایگانیِ_تاریخ_بدونِ_اسلاگ_۴۰۴_می‌ماند() {
        publishedArticle("01");   // حتی اگر چنین اسلاگی بود
        assertNull(fallback.resolve(notFound("/2025/05/01/")));
        assertNull(fallback.resolve(notFound("/2025/05/")));
    }

    // ── قاعدهٔ ۳ ──────────────────────────────────────────────────────────
    @Test
    void مسیرِ_محصولِ_قدیمی_به_آدرسِ_هیبرید() {
        Product p = new Product();
        p.setId("p1");
        p.setSlug("qrt-5");
        // دُمِ فارسی مشتق است، نه فیلد: از persianSlug/name ساخته می‌شود
        p.setPersianSlug("میکروتیک QRT 5");
        when(productRepo.findBySlug("qrt-5")).thenReturn(Optional.of(p));

        String target = fallback.resolve(notFound("/product/qrt-5/"));
        assertNotNull(target);
        assertTrue(target.startsWith("/shop/product/qrt-5/"), "باید آدرسِ هیبریدِ کامل باشد: " + target);
    }

    // ── گاردها ───────────────────────────────────────────────────────────
    @Test
    void مسیرِ_بی‌معنا_۴۰۴_می‌ماند() {
        assertNull(fallback.resolve(notFound("/in-adres-vojud-nadarad/")));
    }

    @Test
    void مسیرهای_ممنوع_دست‌نخورده_می‌مانند() {
        publishedArticle("robots.txt");   // حتی اگر چنین مقاله‌ای بود هم نباید دست بخورد
        assertNull(fallback.resolve(notFound("/api/v1/legacy-redirects")), "API");
        assertNull(fallback.resolve(notFound("/l/abc123")), "لینکِ کوتاه");
        assertNull(fallback.resolve(notFound("/robots.txt")), "robots");
        assertNull(fallback.resolve(notFound("/sitemap.xml")), "نقشهٔ سایت");
        assertNull(fallback.resolve(notFound("/Admin.html")), "پنل");
        assertNull(fallback.resolve(notFound("/css/site-nav.css")), "استاتیک");
        assertNull(fallback.resolve(notFound("/uploads/x.png")), "آپلودها");
        assertNull(fallback.resolve(notFound("/")), "ریشه");
    }

    @Test
    void فقط_خواندن_و_فقط_۴۰۴() {
        publishedArticle("keyloggers");

        MockHttpServletRequest post = new MockHttpServletRequest("POST", "/error");
        post.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/keyloggers/");
        post.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        post.setAttribute("jakarta.servlet.error.method", "POST");
        assertNull(fallback.resolve(post), "POST نباید به ۳۰۱ تبدیل شود");

        MockHttpServletRequest err500 = notFound("/keyloggers/");
        err500.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
        assertNull(fallback.resolve(err500), "فقط ۴۰۴ فالبک می‌گیرد");
    }

    /**
     * 🔴 حلقهٔ ۳۰۱ِ بی‌پایان — روی پراد سنجیده شد و با همین چهار آدرس بازتولید می‌شود.
     * <p>
     * قاعدهٔ ۱ با <b>الگو</b> تطبیق می‌داد، پس «/blog/هرچه» هندلر داشت حتی وقتی آن
     * مقاله وجود نداشت؛ مقصد خودِ همان مسیر می‌شد و مرورگر تا ابد ۳۰۱ می‌گرفت.
     * گوگل این‌ها را «Redirect error» گزارش می‌کند، نه «صفحه حذف شد» — یعنی آدرسِ
     * مرده هیچ‌وقت از ایندکس پاک نمی‌شود.
     */
    @Test
    void آدرسِ_ناموجودِ_زیرِ_الگو_۴۰۴_می‌ماند_نه_حلقه() {
        assertNull(fallback.resolve(notFound("/blog/nabashad-xyz-123")), "مقالهٔ ناموجود");
        assertNull(fallback.resolve(notFound("/blog/hub/nabashad-xyz-123")), "هابِ ناموجود");
        assertNull(fallback.resolve(notFound("/shop/product/nabashad-xyz-123")), "محصولِ ناموجود");
        assertNull(fallback.resolve(notFound("/shop/category/nabashad-xyz")), "دستهٔ ناموجود");
    }

    /**
     * ⚠️ تلهٔ رفع: اگر مقایسهٔ «آیا مسیر اصلاح شد؟» روی رشتهٔ خام انجام می‌شد، هر
     * آدرسِ فارسیِ درصد-کدشده «عوض‌شده» حساب می‌شد و حلقه برمی‌گشت. مقایسه باید روی
     * شکلِ دیکدشده باشد.
     */
    @Test
    void مسیرِ_فارسیِ_درصد_کدشدهٔ_بی‌اسلش_حلقه_نمی‌سازد() {
        // /blog/نبود-چنین-چیزی — بدونِ اسلشِ پایانی، مقاله هم وجود ندارد
        assertNull(fallback.resolve(notFound(
                "/blog/%D9%86%D8%A8%D9%88%D8%AF-%DA%86%D9%86%DB%8C%D9%86-%DA%86%DB%8C%D8%B2%DB%8C")));
    }

    /** دلیلِ وجودیِ قاعدهٔ ۱ نباید قربانی شود: اسلشِ پایانی هنوز یک پرش می‌گیرد. */
    @Test
    void اسلشِ_پایانیِ_مسیرِ_الگودار_هنوز_یک_پرش_می‌گیرد() {
        assertEquals("/blog/har-che", fallback.resolve(notFound("/blog/har-che/")));
        assertEquals("/shop/product/har-che", fallback.resolve(notFound("/shop/product/har-che/")));
    }

    @Test
    void حلقه_ساخته_نمی‌شود() {
        // ریدایرکتی که به خودش برمی‌گردد نباید ۳۰۱ بسازد
        redirect("/blog/x", "/x");
        assertNull(fallback.resolve(notFound("/x/")), "مقصدی که با خودِ مسیر یکی است حلقه است");
    }
}
