package org.example.shop1.controller;

import jakarta.servlet.http.HttpServletRequest; // اضافه شد
import org.example.shop1.model.entity.Article;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.entity.Category;
import org.example.shop1.model.reposritory.ProductRepository;
import org.example.shop1.model.reposritory.CategoryRepository;
import org.example.shop1.model.service.ArticleService;
import org.example.shop1.model.service.BannerService;
import org.example.shop1.model.service.CategoryService;
import org.example.shop1.model.service.CourseService;
import org.example.shop1.model.service.EducationArchiveService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Controller
public class StoreWebController {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final CategoryService categoryService;
    private final ArticleService articleService;
    private final BannerService bannerService;
    private final EducationArchiveService educationArchiveService;
    private final CourseService courseService;
    private final org.example.shop1.model.service.ImageRowBannerService imageRowBannerService;
    private final org.example.shop1.model.service.ProductRedirectService productRedirectService;
    private final org.example.shop1.model.service.LegacyRedirectService legacyRedirectService;

    // فاز ۰ رودمپ: آنالیتیکس — خالی بودن یعنی تگ رندر نمی‌شود
    @org.springframework.beans.factory.annotation.Value("${analytics.ga4.measurement-id:}")
    private String ga4Id;

    @org.springframework.beans.factory.annotation.Value("${analytics.gsc.verification:}")
    private String gscToken;

    public StoreWebController(ProductRepository productRepo, CategoryRepository categoryRepo,
                              CategoryService categoryService, ArticleService articleService,
                              BannerService bannerService, EducationArchiveService educationArchiveService,
                              CourseService courseService,
                              org.example.shop1.model.service.ImageRowBannerService imageRowBannerService,
                              org.example.shop1.model.service.ProductRedirectService productRedirectService,
                              org.example.shop1.model.service.LegacyRedirectService legacyRedirectService) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.categoryService = categoryService;
        this.articleService = articleService;
        this.bannerService = bannerService;
        this.educationArchiveService = educationArchiveService;
        this.courseService = courseService;
        this.imageRowBannerService = imageRowBannerService;
        this.productRedirectService = productRedirectService;
        this.legacyRedirectService = legacyRedirectService;
    }

    // ================= صفحه‌ی داده نما (ریشه‌ی سایت) =================
    // 🔴 قبلاً «/» فروشگاه بود؛ حالا صفحه‌ی معرفیِ شرکت است (تصمیمِ معماریِ
    // داده‌نما/فروشگاه/آموزش، جزئیات: docs/prompt-tech-chat-dadehnama-home.md).
    // کاملاً SSR — بیشترین وزنِ سئویی رویِ کلِ سایت را دارد.
    @GetMapping("/")
    public String dadehNamaHomePage(Model model, HttpServletRequest request) {
        addDynamicUrls(model, request);

        String baseUrl = buildBaseUrl(request);
        String canonical = baseUrl + "/";

        String description = "از سال ۱۳۸۰ در ارتباطاتِ بی‌سیم، نمایندگیِ میکروتیک، لینک‌هایِ رادیوییِ پرظرفیت و "
                + "برگزاریِ دوره‌ها و سمینارهایِ تخصصیِ شبکه در ایران — دفترِ مرکزی اصفهان و دفترِ تهران.";

        model.addAttribute("seoTitle", "داده نما | ارتباطاتِ بی‌سیم، نمایندگیِ میکروتیک و آموزشِ تخصصیِ شبکه");
        model.addAttribute("seoDescription", description);
        model.addAttribute("canonicalUrl", canonical);
        model.addAttribute("organizationJsonLd", buildStandaloneOrganizationJsonLd(baseUrl, description));

        // اسلایدرِ بنرِ بالایِ صفحه — همان سیستمِ بنرگذاریِ فروشگاه، جایگاهِ جدا (HOME_HERO).
        // لینک‌ها همین‌جا resolve می‌شوند (نه در زمانِ ذخیره) تا تغییرِ بعدیِ محصول/دسته/مقاله لینک را نشکند.
        model.addAttribute("banners", bannerService.getHomeBannersResolved());

        // نوارِ «کارنامه» — پنلِ دوره‌ها با دادهٔ واقعی: مجموعِ زنده از دیتابیس +
        // نردبانِ گواهی‌نامه‌هایِ میکروتیک (اعدادِ مستندشده در docs/company-profile-source.md).
        model.addAttribute("heroCourseCountFa",
                faDigits(String.valueOf(articleService.countHubArticles(COURSES_HUB_SLUG))));

        // بلاکِ خلاصه‌ی «سابقه و اعتبار» — قبلاً در فروشگاه بود، طبقِ خواسته‌ی مالک
        // به همین صفحه (خانه‌ی داده‌نما) منتقل شد. شمارشِ دوره از هَمین heroCourseCountFa
        // بالا استفاده می‌شود (محاسبه‌ی تکراری لازم نیست).
        model.addAttribute("trustSinceFa", ONLINE_SINCE_JALALI);
        model.addAttribute("trustYearsFa", faDigits(String.valueOf(
                java.time.Period.between(ONLINE_SINCE, java.time.LocalDate.now()).getYears())));

        model.addAttribute("certLadder", CERT_LADDER);
        model.addAttribute("certLadderTotalFa", faDigits(String.valueOf(
                CERT_LADDER.stream().mapToInt(CertRung::getCount).sum())));

        // فهرستِ مقالاتِ منتشرشده‌ی هابِ «دوره‌های آموزشی» — زیرِ نردبانِ گواهی‌نامه‌ها،
        // داخلِ یک جعبه‌ی قابلِ‌اسکرول (خواسته‌ی مالک)
        model.addAttribute("courseHubArticles", articleService.getHubArticles(COURSES_HUB_SLUG));

        // پنجرهٔ آرشیوِ تصویریِ کلاس‌ها — بلافاصله زیرِ نردبان/فهرستِ بالا، لینک به /learn
        model.addAttribute("educationArchiveItems", educationArchiveService.getActive());

        // تعدادِ محصولاتِ جعبهٔ خرید — زنده از دیتابیس (نه عددِ ثابت که با اضافه/حذفِ محصول قدیمی می‌شود)
        model.addAttribute("productCountFa", faDigits(String.valueOf(productRepo.count())));

        // پنجره‌هایِ ردیفیِ بنرِ تصویری — جایگاهِ HOME
        model.addAttribute("imageRowBanners", imageRowBannerService.getActiveByPlacement("HOME"));

        return "home";
    }

    /**
     * نردبانِ گواهی‌نامه‌هایِ میکروتیک برایِ پنلِ «دوره‌ها»یِ صفحه‌ی داده‌نما — عمداً همینجا
     * ثابت است، نه کوئریِ زنده‌یِ تطبیقِ عنوان: دیتای خام (عنوانِ ۴۹ مقاله) به‌قدرِ کافی
     * ناهم‌سان است که تطبیقِ خودکار می‌توانست دوباره همان خطایِ «تعمیمِ نمونه به کل» را
     * تکرار کند. این پنج عدد قبلاً با شمارشِ کاملِ آرشیو تأیید شده‌اند —
     * منبع و روشِ شمارش: docs/company-profile-source.md (بخشِ ۳).
     */
    private static final List<CertRung> CERT_LADDER = List.of(
            new CertRung("MTCNA", 12, "مقدماتی"),
            new CertRung("MTCWE", 6, "وایرلس"),
            new CertRung("MTCTCE", 5, "کنترل ترافیک"),
            new CertRung("MTCRE", 4, "مسیریابی"),
            new CertRung("MTCUME", 1, "مدیریت کاربران")
    );

    public static class CertRung {
        private final String code, subtitle;
        private final int count;

        CertRung(String code, int count, String subtitle) {
            this.code = code; this.count = count; this.subtitle = subtitle;
        }

        public String getCode() { return code; }
        public int getCount() { return count; }
        public String getCountFa() { return faDigits(String.valueOf(count)); }
        public String getSubtitle() { return subtitle; }
    }

    // نمایِ فروشگاه (سرچ‌بار، بنر، سکشن‌ها، گریدِ محصولات) — قبلاً روی «/» بود، حالا زیرِ
    // /shop تا سه بخشِ سایت (داده‌نما/فروشگاه/آموزش) مرزِ روشن داشته باشند.
    @GetMapping("/shop")
    public String shopHomePage(Model model, HttpServletRequest request) {
        // متد کمکی برای اضافه کردن آدرس‌های زنده (جلوگیری از خطای ۵۰۰)
        addDynamicUrls(model, request);

        model.addAttribute("seoTitle", "فروشگاه داده نما | میکروتیک، رادیو وایرلس و تجهیزاتِ شبکه");
        model.addAttribute("seoDescription", "فروشگاه اینترنتی یاس، عرضه کننده بهترین محصولات با گارانتی معتبر و ارسال فوری");
        model.addAttribute("canonicalUrl", buildBaseUrl(request) + "/shop");

        // اسلایدرِ بنر — بالاترینِ عنصرِ صفحه (LCP)؛ لینک‌ها همین‌جا (نه در زمانِ
        // ذخیره‌ی بنر) resolve می‌شوند تا تغییرِ بعدیِ نامِ محصول/دسته/مقاله لینک را نشکند.
        model.addAttribute("banners", bannerService.getHeroBannersResolved());

        // پنجره‌هایِ ردیفیِ بنرِ تصویری — فقط جایگاهِ بالایِ صفحه (SHOP_TOP)؛ حالتِ
        // SHOP_AFTER_SECTION مثلِ AFTER_SECTIONِ بنر، کاملاً کلاینتی رندر می‌شود
        // (کنارِ سکشن‌های لندینگ که خودشان هم SSR نیستند).
        model.addAttribute("imageRowBanners", imageRowBannerService.getActiveByPlacement("SHOP_TOP"));

        return "CL";
    }

    // آدرسِ قدیمیِ تک‌بخشی — برای سازگاریِ عقب همچنان ۲۰۰ می‌دهد؛ canonical به فرمِ هیبرید هدایت می‌کند
    @GetMapping("/shop/product/{slugOrId}")
    public Object productPage(@PathVariable String slugOrId, Model model, HttpServletRequest request) {
        addDynamicUrls(model, request);
        Optional<Product> found = resolveProduct(slugOrId);
        if (found.isEmpty()) {
            // محصول نیست: شاید اسلاگش بعدِ ادغام ریدایرکت شده باشد (۳۰۱ به‌جایِ ۴۰۴)
            Object redirect = redirectOrNull(slugOrId, request);
            if (redirect != null) return redirect;
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "محصول یافت نشد");
        }
        Object moved = discontinuedRedirectOrNull(found.get(), request);
        if (moved != null) return moved;
        populateProductModel(found.get(), model, request);
        return "CL";
    }

    /**
     * اگر اسلاگ در {@code product_redirects} مقصدِ زنده داشته باشد، ۳۰۱ به آدرسِ
     * canonicalِ مقصد؛ وگرنه {@code null} تا فراخوان ۴۰۴ واقعی بدهد.
     * <p>
     * ⚠️ فقط بعد از شکستِ {@code resolveProduct} صدا زده می‌شود — یعنی محصولِ زنده همیشه
     * مقدم است و یک ریدایرکتِ اشتباه نمی‌تواند صفحهٔ سالمی را بدزدد.
     */
    /**
     * اسلاگِ قدیمیِ مقاله — اگر در {@code legacy_redirects} مقصدی داشته باشد، ۳۰۱؛
     * وگرنه {@code null} تا فراخوان ۴۰۴ واقعی بدهد.
     * <p>
     * 🔴 <b>چرا اینجا و نه در {@code LegacyRedirectFilter}:</b> آن فیلتر پیش از
     * مسیریابیِ اسپرینگ اجرا می‌شود و نمی‌داند مقالهٔ زنده‌ای با این اسلاگ هست یا نه.
     * اینجا می‌دانیم که نیست، چون تازه شکست خورده‌ایم. برای همین
     * {@code isLateResolved} این مسیرها را از آن فیلتر کنار می‌گذارد و تنها راهِ
     * اعمالشان همین‌جاست.
     * <p>
     * ⚠️ {@code Location} با {@code toLocationHeader} ساخته می‌شود، نه با مسیرِ خام:
     * اسلاگ‌ها فارسی‌اند و هدرِ HTTP نویسهٔ غیرِ لاتین-۱ نمی‌پذیرد — تامکت هدر را
     * بی‌صدا حذف می‌کند و نتیجه ۳۰۱ِ بدونِ مقصد می‌شود.
     */
    private Object articleRedirectOrNull(String slugOrId) {
        Optional<org.example.shop1.model.entity.LegacyRedirect> hit =
                legacyRedirectService.resolve("/blog/" + slugOrId);
        if (hit.isEmpty()) return null;

        String location = org.example.shop1.model.service.LegacyRedirectService
                .toLocationHeader(hit.get().getToPath());
        legacyRedirectService.countHit(hit.get().getFromPath());

        org.springframework.web.servlet.view.RedirectView rv =
                new org.springframework.web.servlet.view.RedirectView(location);
        rv.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        return rv;
    }

    /**
     * محصولِ متوقف‌شده با جایگزینِ زنده → ۳۰۱ به جایگزین؛ وگرنه {@code null} تا صفحهٔ
     * خودش (۲۰۰) نشان داده شود.
     * <p>
     * 🔴 <b>چرا اینجا و نه در {@code product_redirects}:</b> آن جدول فقط وقتی خوانده
     * می‌شود که محصول <i>پیدا نشود</i>. محصولِ متوقف‌شده حذف نشده — زنده است و پیدا
     * می‌شود — پس آن مسیر هرگز به کار نمی‌آمد.
     * <p>
     * اگر کارشناس «نمایشِ پیام» را روشن کرده باشد، شناسهٔ محصولِ مبدأ با
     * {@code ?replaced=} همراه می‌شود تا صفحهٔ مقصد بگوید «این جایگزینِ آن است».
     * وقتی پیام خاموش است آدرس تمیز می‌ماند — تمیزترین شکل برای سئو پیش‌فرض است.
     * canonicalِ صفحهٔ مقصد به‌هرحال بدونِ این پارامتر است.
     */
    private Object discontinuedRedirectOrNull(Product p, HttpServletRequest request) {
        Optional<Product> target = liveReplacementOf(p);
        if (target.isEmpty()) return null;

        String location = buildBaseUrl(request) + hybridPathEncoded(target.get());
        if (p.isDiscontinuedNoticeShown()) location += "?replaced=" + p.getId();

        org.springframework.web.servlet.view.RedirectView rv =
                new org.springframework.web.servlet.view.RedirectView(location);
        rv.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        return rv;
    }

    /**
     * اولین جایگزینِ <b>زنده</b> در زنجیره.
     * <p>
     * جایگزین خودش ممکن است بعدها متوقف شود، پس زنجیره دنبال می‌شود — با سقفِ پرش و
     * مجموعهٔ دیده‌شده. حلقه، جایگزینِ حذف‌شده، یا زنجیرهٔ بیش از حد بلند همه به
     * «جایگزین نداریم» می‌رسند و صفحهٔ خودِ محصول ۲۰۰ می‌دهد — هرگز ۴۰۴ یا حلقهٔ
     * ریدایرکت.
     */
    private Optional<Product> liveReplacementOf(Product p) {
        if (p == null || !p.isProductionStopped()) return Optional.empty();
        java.util.Set<String> seen = new java.util.HashSet<>();
        seen.add(p.getId());
        String cursor = p.getReplacementProductId();
        for (int hop = 0;
             hop < org.example.shop1.model.service.ProductService.MAX_REPLACEMENT_HOPS && cursor != null;
             hop++) {
            if (!seen.add(cursor)) return Optional.empty();
            Product next = productRepo.findById(cursor).orElse(null);
            if (next == null) return Optional.empty();
            if (!next.isProductionStopped()) return Optional.of(next);
            cursor = next.getReplacementProductId();
        }
        return Optional.empty();
    }

    private Object redirectOrNull(String slug, HttpServletRequest request) {
        Optional<Product> target = productRedirectService.resolveTarget(slug);
        if (target.isEmpty()) return null;

        // Location باید ASCII باشد؛ همان فرمِ percent-encodeِ ریدایرکتِ دُمِ فارسی
        String canonical = buildBaseUrl(request) + hybridPathEncoded(target.get());
        org.springframework.web.servlet.view.RedirectView rv =
                new org.springframework.web.servlet.view.RedirectView(canonical);
        rv.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        return rv;
    }

    // آدرسِ هیبریدِ جدید: /shop/product/{resolver}/{persianTail}
    // resolver محصول را قطعی resolve می‌کند؛ دُم فقط تزئینی/سئو است و برای lookup نادیده گرفته می‌شود.
    @GetMapping("/shop/product/{resolver}/{persianTail}")
    public Object productPageHybrid(@PathVariable String resolver, @PathVariable String persianTail,
                                    Model model, HttpServletRequest request) {
        addDynamicUrls(model, request);
        Optional<Product> found = resolveProduct(resolver);
        if (found.isEmpty()) {
            // همان منطقِ مسیرِ تک‌بخشی: اسلاگِ ادغام‌شده ۳۰۱ می‌گیرد، نه ۴۰۴.
            // دُمِ فارسیِ آدرسِ قدیمی عمداً دور ریخته می‌شود — مقصد دُمِ خودش را دارد.
            Object redirect = redirectOrNull(resolver, request);
            if (redirect != null) return redirect;
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "محصول یافت نشد");
        }
        Product p = found.get();

        // توقفِ تولید پیش از تصحیحِ دُم: اگر جایگزین دارد، یک‌راست به آن برو. اگر اول
        // دُم تصحیح می‌شد، بازدیدکننده دو پرش می‌خورد — یکی به دُمِ درست و یکی به جایگزین.
        Object moved = discontinuedRedirectOrNull(p, request);
        if (moved != null) return moved;

        // اگر دُم غلط/کهنه بود، ۳۰۱ به فرمِ canonicalِ درست (تثبیتِ آدرس + جلوگیری از محتوای تکراری)
        String correctTail = p.getPersianTail();
        if (correctTail != null && !correctTail.isEmpty() && !correctTail.equals(persianTail)) {
            // Location باید ASCII باشد؛ فرمِ percent-encode شده
            String canonical = buildBaseUrl(request) + hybridPathEncoded(p);
            org.springframework.web.servlet.view.RedirectView rv =
                    new org.springframework.web.servlet.view.RedirectView(canonical);
            rv.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
            return rv;
        }

        populateProductModel(p, model, request);
        return "CL";
    }

    // resolve پایدار: اول با اسلاگ، بعد با شناسه
    private Optional<Product> resolveProduct(String resolver) {
        Optional<Product> productOpt = productRepo.findBySlug(resolver);
        if (productOpt.isEmpty()) productOpt = productRepo.findById(resolver);
        return productOpt;
    }

    // ================= صفحه‌ی جزئیاتِ دوره — همان قالبِ CL.html (SPA) =================
    // 🔴 دوره از همان CL.html رندر می‌شود (نه قالبِ جدا) چون خریدش باید از همان سبد/تسویه‌حسابِ
    // فروشگاه رد شود؛ ساختنِ صفحه‌ی مستقل یعنی بازسازیِ کارتِ خرید برایِ دوره.
    @GetMapping("/shop/course/{slugOrId}")
    public Object coursePage(@PathVariable String slugOrId, Model model, HttpServletRequest request) {
        addDynamicUrls(model, request);
        Optional<org.example.shop1.model.entity.Course> found = courseService.findBySlugOrId(slugOrId);
        if (found.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "دوره یافت نشد");
        }
        populateCourseModel(found.get(), model, request);
        return "CL";
    }

    private void populateCourseModel(org.example.shop1.model.entity.Course c, Model model, HttpServletRequest request) {
        String baseUrl = buildBaseUrl(request);
        model.addAttribute("course", c);
        model.addAttribute("seoTitle", c.getSeoTitle() != null ? c.getSeoTitle() : c.getTitle());
        model.addAttribute("seoDescription", c.getSeoDescription() != null ? c.getSeoDescription()
                : "ثبت‌نامِ دوره‌ی " + c.getTitle() + " در داده‌نما");
        String slug = (c.getSlug() != null && !c.getSlug().isEmpty()) ? c.getSlug() : c.getId();
        model.addAttribute("canonicalUrl", baseUrl + "/shop/course/" + slug);
        if (c.getBannerImage() != null && !c.getBannerImage().isEmpty()) {
            model.addAttribute("ogImage", c.getBannerImage().startsWith("http") ? c.getBannerImage() : baseUrl + c.getBannerImage());
        } else if (c.getImages() != null && !c.getImages().isEmpty()) {
            String img = c.getImages().get(0);
            model.addAttribute("ogImage", img.startsWith("http") ? img : baseUrl + img);
        }
        // resolver پایدار برایِ SPA (window.SERVER_COURSE_SLUG)
        model.addAttribute("courseSlug", slug);

        // اسکیمای FAQPage (همان الگویِ محصول — فقط وقتی پرسش متداولِ واقعی وجود دارد)
        if (c.getFaqs() != null && !c.getFaqs().isEmpty()) {
            StringBuilder fq = new StringBuilder();
            fq.append("{\"@context\":\"https://schema.org/\",\"@type\":\"FAQPage\",\"mainEntity\":[");
            for (int i = 0; i < c.getFaqs().size(); i++) {
                var f = c.getFaqs().get(i);
                if (i > 0) fq.append(",");
                fq.append("{\"@type\":\"Question\",\"name\":\"").append(esc(f.getQuestion()))
                        .append("\",\"acceptedAnswer\":{\"@type\":\"Answer\",\"text\":\"")
                        .append(esc(f.getAnswer())).append("\"}}");
            }
            fq.append("]}");
            model.addAttribute("faqJsonLd", fq.toString());
        }
    }

    // این سه تابع به ProductUrlUtil منتقل شدند تا Torob API هم دقیقاً همان آدرسِ
    // canonical را بسازد، نه یک آدرسِ موازی. اینجا فقط delegate می‌کنیم.
    private String productResolver(Product p) {
        return org.example.shop1.model.service.util.ProductUrlUtil.productResolver(p);
    }

    private String hybridPath(Product p) {
        return org.example.shop1.model.service.util.ProductUrlUtil.hybridPath(p);
    }

    private String hybridPathEncoded(Product p) {
        return org.example.shop1.model.service.util.ProductUrlUtil.hybridPathEncoded(p);
    }

    // پر کردن مدلِ صفحه‌ی محصول + canonical/og/productSlug روی فرمِ هیبریدِ درست
    private void populateProductModel(Product p, Model model, HttpServletRequest request) {
        String baseUrl = buildBaseUrl(request);
        model.addAttribute("p", p);
        model.addAttribute("seoTitle", p.getSeoTitle() != null ? p.getSeoTitle() : p.getName());
        model.addAttribute("seoDescription", p.getSeoDescription() != null ? p.getSeoDescription() : "خرید آنلاین محصول " + p.getName());

        // آدرس مطلق تصویر برای og:image و JSON-LD (تصاویر در دیتابیس نسبی‌اند: /uploads/..)
        if (p.getImages() != null && !p.getImages().isEmpty() && p.getImages().get(0) != null) {
            String img = p.getImages().get(0);
            model.addAttribute("ogImage", img.startsWith("http") ? img : baseUrl + img);
        }

        // اعتبار قیمت برای اسکیما: یک سال بعد (به‌جای تاریخ هاردکد)
        model.addAttribute("priceValidUntil", java.time.LocalDate.now().plusYears(1).toString());

        // aggregateRating فقط وقتی نظر واقعی وجود دارد (جلوگیری از امتیاز جعلی/جریمه گوگل)
        String aggregateRatingJson = "";
        if (p.getReviewCount() != null && p.getReviewCount() > 0) {
            double rating = (p.getAverageRating() != null && p.getAverageRating() > 0) ? p.getAverageRating() : 5;
            aggregateRatingJson = ",\"aggregateRating\":{\"@type\":\"AggregateRating\",\"ratingValue\":\""
                    + rating + "\",\"reviewCount\":\"" + p.getReviewCount() + "\"}";
        }
        model.addAttribute("aggregateRatingJson", aggregateRatingJson);

        // resolver پایدار برای SPA (window.SERVER_PRODUCT_SLUG)
        model.addAttribute("productSlug", productResolver(p));

        // canonical/og:url همیشه فرمِ هیبریدِ درست (جلوگیری از ایندکس دوگانه)
        model.addAttribute("canonicalUrl", baseUrl + hybridPath(p));

        // اسکیمای FAQPage (فقط وقتی پرسش متداول واقعی وجود دارد)
        if (p.getFaqs() != null && !p.getFaqs().isEmpty()) {
            StringBuilder fq = new StringBuilder();
            fq.append("{\"@context\":\"https://schema.org/\",\"@type\":\"FAQPage\",\"mainEntity\":[");
            for (int i = 0; i < p.getFaqs().size(); i++) {
                var f = p.getFaqs().get(i);
                if (i > 0) fq.append(",");
                fq.append("{\"@type\":\"Question\",\"name\":\"").append(esc(f.getQuestion()))
                        .append("\",\"acceptedAnswer\":{\"@type\":\"Answer\",\"text\":\"")
                        .append(esc(f.getAnswer())).append("\"}}");
            }
            fq.append("]}");
            model.addAttribute("faqJsonLd", fq.toString());
        }

        String catName = "داده نما";
        if (p.getCategoryId() != null) {
            catName = categoryRepo.findById(p.getCategoryId())
                    .map(Category::getName)
                    .orElse("داده نما");
        }
        model.addAttribute("categoryName", catName);

        model.addAttribute("productJsonLd", buildProductJsonLd(p, baseUrl,
                baseUrl + hybridPath(p), (String) model.getAttribute("seoDescription"),
                (String) model.getAttribute("ogImage"), aggregateRatingJson, catName));
    }

    /**
     * اسکیمای محصول — سمتِ سرور، مثلِ اسکیمای دسته و FAQ.
     * <p>
     * 🔴 <b>چرا از قالب بیرون آمد:</b> حالا سه فیلد (brand، sku، mpn) باید وقتی
     * مقدار ندارند <b>اصلاً نوشته نشوند</b>، نه اینکه رشتهٔ تهی بگیرند. ساختنِ
     * JSONِ شرطی با {@code th:if} داخلِ متنِ اینلاین یعنی ویرگولِ اضافه و JSONِ
     * نامعتبر — همان دلیلی که اسکیمای دسته از اول سروری نوشته شد.
     */
    private String buildProductJsonLd(Product p, String baseUrl, String canonical,
                                      String description, String image,
                                      String aggregateRatingJson, String categoryName) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"@context\":\"https://schema.org/\",\"@graph\":[{\"@type\":\"Product\"")
                .append(",\"name\":\"").append(esc(p.getName())).append("\"");
        if (image != null && !image.isBlank()) {
            sb.append(",\"image\":\"").append(esc(image)).append("\"");
        }
        sb.append(",\"description\":\"").append(esc(description)).append("\"");

        // 🔴 sku کدِ کالایِ خودمان است و mpn کدِ مدلِ سازنده. تا پیش از این هر دو
        // شناسهٔ مونگو بودند — یعنی یک رشتهٔ هگزِ بی‌معنا که به هیچ کاتالوگی نمی‌خورد.
        String sku = blank(p.getHolooCode());
        if (sku != null) sb.append(",\"sku\":\"").append(esc(sku)).append("\"");
        String mpn = manufacturerPartNumber(p);
        if (mpn != null) sb.append(",\"mpn\":\"").append(esc(mpn)).append("\"");

        // ⚠️ برندِ نامعلوم یعنی حذفِ کاملِ فیلد، نه نامِ فروشگاه. برندِ غلط سیگنالِ
        // سازنده را خراب می‌کند و در نتایجِ خرید به ضررمان است.
        String brand = brandOf(p);
        if (brand != null) {
            sb.append(",\"brand\":{\"@type\":\"Brand\",\"name\":\"").append(esc(brand)).append("\"}");
        }

        java.math.BigDecimal price = p.getOnlinePrice() != null ? p.getOnlinePrice() : p.getPrice();
        sb.append(",\"offers\":{\"@type\":\"Offer\"")
                .append(",\"url\":\"").append(esc(canonical)).append("\"")
                .append(",\"priceCurrency\":\"IRR\"")
                // ×۱۰ چون قیمت‌های ما تومان‌اند و واحدِ اعلام‌شده ریال است
                .append(",\"price\":\"").append(price == null ? "0"
                        : price.setScale(0, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.TEN).toPlainString()).append("\"")
                .append(",\"validFrom\":\"").append(priceValidFrom(p)).append("\"")
                .append(",\"priceValidUntil\":\"").append(java.time.LocalDate.now().plusYears(1)).append("\"")
                .append(",\"itemCondition\":\"https://schema.org/NewCondition\"")
                .append(",\"availability\":\"").append(availabilityOf(p)).append("\"}");

        if (aggregateRatingJson != null && !aggregateRatingJson.isEmpty()) {
            sb.append(aggregateRatingJson);
        }
        sb.append("}");

        sb.append(",{\"@type\":\"BreadcrumbList\",\"itemListElement\":[")
                .append("{\"@type\":\"ListItem\",\"position\":1,\"name\":\"فروشگاه\",\"item\":\"")
                .append(esc(baseUrl)).append("/shop\"},")
                .append("{\"@type\":\"ListItem\",\"position\":2,\"name\":\"").append(esc(categoryName))
                .append("\",\"item\":\"").append(esc(canonical)).append("\"}]}");

        return sb.append("]}").toString();
    }

    private static String availabilityOf(Product p) {
        if (p.isProductionStopped()) return "https://schema.org/Discontinued";
        return (p.getStock() != null && p.getStock() > 0)
                ? "https://schema.org/InStock" : "https://schema.org/OutOfStock";
    }

    /** کدِ مدلِ سازنده از مشخصاتِ فنی. امروز فقط رویِ بخشی از محصولات هست. */
    private static String manufacturerPartNumber(Product p) {
        if (p.getSpecifications() == null) return null;
        return blank(p.getSpecifications().get("Product code"));
    }

    /**
     * تاریخِ شروعِ اعتبارِ قیمت — آخرین باری که خودِ محصول به‌روز شد.
     * <p>
     * دقیق‌ترین چیزی است که داریم: فیلدِ جدایی برایِ «زمانِ تغییرِ قیمت» وجود ندارد،
     * و ساختنِ تاریخِ دلخواه یعنی به گوگل عددِ بی‌پشتوانه دادن.
     */
    private static String priceValidFrom(Product p) {
        java.time.Instant t = p.getUpdatedAt() != null ? p.getUpdatedAt() : p.getCreatedAt();
        if (t == null) t = java.time.Instant.now();
        return t.atZone(java.time.ZoneOffset.UTC).toLocalDate().toString();
    }

    /**
     * برندِ محصول از نزدیک‌ترین دسته‌ای که {@code brandName} دارد.
     * <p>
     * از خودِ دسته شروع می‌کند و بعد نیاها را از نزدیک به دور بالا می‌رود — چون
     * {@code ancestors} از ریشه به پایین ذخیره می‌شود، از آخر به اول پیمایش می‌شود.
     * اگر هیچ‌کدام برند نداشتند، {@code null} یعنی «ننویس».
     */
    private String brandOf(Product p) {
        if (p.getCategoryId() == null) return null;
        Optional<Category> own = categoryRepo.findById(p.getCategoryId());
        if (own.isEmpty()) return null;

        String direct = blank(own.get().getBrandName());
        if (direct != null) return direct;

        List<String> ancestors = own.get().getAncestors();
        if (ancestors == null) return null;
        for (int i = ancestors.size() - 1; i >= 0; i--) {
            String found = categoryRepo.findById(ancestors.get(i))
                    .map(Category::getBrandName).map(StoreWebController::blank).orElse(null);
            if (found != null) return found;
        }
        return null;
    }

    private static String blank(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // ================= صفحه‌ی سئوی دسته‌بندی =================
    @GetMapping("/shop/category/{slugOrId}")
    public String categoryPage(@PathVariable String slugOrId, Model model, HttpServletRequest request) {
        addDynamicUrls(model, request);

        Category cat = categoryService.findBySlugOrId(slugOrId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "دسته یافت نشد"));

        String baseUrl = buildBaseUrl(request);
        String slug = (cat.getSlug() != null && !cat.getSlug().isEmpty()) ? cat.getSlug() : cat.getId();
        String canonical = baseUrl + "/shop/category/" + slug;

        String title = (cat.getSeoTitle() != null && !cat.getSeoTitle().isEmpty())
                ? cat.getSeoTitle()
                : "خرید " + cat.getName() + " | فروشگاه داده نما";
        String description = (cat.getSeoDescription() != null && !cat.getSeoDescription().isEmpty())
                ? cat.getSeoDescription()
                : "خرید انواع " + cat.getName() + " با بهترین قیمت و گارانتی معتبر از فروشگاه داده نما";

        model.addAttribute("cat", cat);
        model.addAttribute("seoTitle", title);
        model.addAttribute("seoDescription", description);
        model.addAttribute("canonicalUrl", canonical);

        // دسته‌بندیِ نوعِ COURSE — به‌جایِ محصول، دوره‌هایِ همان دسته (نگاه کن به
        // Course.categoryId که در تسکِ دسته‌بندیِ آموزش اضافه شد)
        if ("COURSE".equalsIgnoreCase(cat.getType())) {
            List<org.example.shop1.model.entity.Course> catCourses = courseService.getByCategoryId(cat.getId());
            model.addAttribute("catCourses", catCourses);
            model.addAttribute("categoryJsonLd", buildCourseCategoryJsonLd(cat, catCourses, baseUrl, canonical, description));
        } else {
            List<Product> catProducts = categoryService.getProductsInSubtree(cat.getId(), 60);
            model.addAttribute("catProducts", catProducts);
            model.addAttribute("categoryJsonLd", buildCategoryJsonLd(cat, catProducts, baseUrl, canonical, description));
        }

        return "CL";
    }

    // ساخت JSON-LD صفحه‌ی دسته‌یِ آموزش (لینک‌ها به /shop/course/{slug}، نه محصول)
    private String buildCourseCategoryJsonLd(Category cat, List<org.example.shop1.model.entity.Course> courses,
                                              String baseUrl, String canonical, String description) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"@context\":\"https://schema.org/\",\"@graph\":[");

        sb.append("{\"@type\":\"CollectionPage\",\"name\":\"").append(esc(cat.getName()))
                .append("\",\"description\":\"").append(esc(description))
                .append("\",\"url\":\"").append(esc(canonical)).append("\"}");

        sb.append(",{\"@type\":\"BreadcrumbList\",\"itemListElement\":[")
                .append("{\"@type\":\"ListItem\",\"position\":1,\"name\":\"فروشگاه\",\"item\":\"")
                .append(esc(baseUrl)).append("/shop\"},")
                .append("{\"@type\":\"ListItem\",\"position\":2,\"name\":\"").append(esc(cat.getName()))
                .append("\",\"item\":\"").append(esc(canonical)).append("\"}]}");

        if (courses != null && !courses.isEmpty()) {
            sb.append(",{\"@type\":\"ItemList\",\"itemListElement\":[");
            int pos = 1;
            int max = Math.min(courses.size(), 30);
            for (int i = 0; i < max; i++) {
                var c = courses.get(i);
                String slug = (c.getSlug() != null && !c.getSlug().isEmpty()) ? c.getSlug() : c.getId();
                if (i > 0) sb.append(",");
                sb.append("{\"@type\":\"ListItem\",\"position\":").append(pos++)
                        .append(",\"url\":\"").append(esc(baseUrl + "/shop/course/" + slug)).append("\"}");
            }
            sb.append("]}");
        }

        sb.append("]}");
        return sb.toString();
    }

    // ساخت JSON-LD صفحه دسته به صورت سروری (CollectionPage + BreadcrumbList + ItemList)
    private String buildCategoryJsonLd(Category cat, List<Product> products,
                                       String baseUrl, String canonical, String description) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"@context\":\"https://schema.org/\",\"@graph\":[");

        sb.append("{\"@type\":\"CollectionPage\",\"name\":\"").append(esc(cat.getName()))
                .append("\",\"description\":\"").append(esc(description))
                .append("\",\"url\":\"").append(esc(canonical)).append("\"}");

        sb.append(",{\"@type\":\"BreadcrumbList\",\"itemListElement\":[")
                .append("{\"@type\":\"ListItem\",\"position\":1,\"name\":\"فروشگاه\",\"item\":\"")
                .append(esc(baseUrl)).append("/shop\"},")
                .append("{\"@type\":\"ListItem\",\"position\":2,\"name\":\"").append(esc(cat.getName()))
                .append("\",\"item\":\"").append(esc(canonical)).append("\"}]}");

        if (products != null && !products.isEmpty()) {
            sb.append(",{\"@type\":\"ItemList\",\"itemListElement\":[");
            int pos = 1;
            int max = Math.min(products.size(), 30);
            for (int i = 0; i < max; i++) {
                Product p = products.get(i);
                if (i > 0) sb.append(",");
                sb.append("{\"@type\":\"ListItem\",\"position\":").append(pos++)
                        .append(",\"url\":\"").append(esc(baseUrl + hybridPath(p))).append("\"}");
            }
            sb.append("]}");
        }

        sb.append("]}");
        return sb.toString();
    }

    // escape امن رشته برای قرارگیری داخل JSON
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", " ").replace("\t", " ");
    }

    // ================= بلاگ (SSR کامل — قطب محتوای آموزشی) =================

    @GetMapping("/blog")
    public String blogList(Model model, HttpServletRequest request,
                           @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "0") int page) {
        addDynamicUrls(model, request);
        org.springframework.data.domain.Page<Article> articles = articleService.getPublished(page, 12);
        model.addAttribute("articles", articles);
        model.addAttribute("cards", articles.getContent());
        model.addAttribute("blogPage", page);
        model.addAttribute("seoTitle", "بلاگ آموزشی میکروتیک و شبکه | داده نما");
        model.addAttribute("seoDescription", "مقالات تخصصی آموزش، عیب‌یابی و راهنمای خرید میکروتیک، وایرلس و تجهیزات شبکه");
        model.addAttribute("canonicalUrl", buildBaseUrl(request) + "/blog");
        return "blog";
    }

    @GetMapping("/blog/{slugOrId}")
    public Object articlePage(@PathVariable String slugOrId, Model model, HttpServletRequest request) {
        addDynamicUrls(model, request);

        Optional<Article> found = articleService.getPublishedBySlugOrId(slugOrId);
        if (found.isEmpty()) {
            // ⚠️ فقط بعد از شکستِ جست‌وجو — یعنی مقالهٔ زنده همیشه مقدم است و یک
            // ریدایرکتِ کهنه نمی‌تواند صفحهٔ سالمی را بدزدد. همان قاعده‌ای که
            // redirectOrNull برای محصول دارد.
            Object moved = articleRedirectOrNull(slugOrId);
            if (moved != null) return moved;
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "مقاله یافت نشد");
        }
        Article a = found.get();

        String baseUrl = buildBaseUrl(request);
        String slug = (a.getSlug() != null && !a.getSlug().isEmpty()) ? a.getSlug() : a.getId();
        String canonical = baseUrl + "/blog/" + slug;

        String title = (a.getSeoTitle() != null && !a.getSeoTitle().isEmpty())
                ? a.getSeoTitle() : a.getTitle() + " | آموزش تخصصی – داده نما";
        String description = (a.getSeoDescription() != null && !a.getSeoDescription().isEmpty())
                ? a.getSeoDescription()
                : (a.getExcerpt() != null && !a.getExcerpt().isEmpty() ? a.getExcerpt() : a.getTitle());

        model.addAttribute("article", a);
        model.addAttribute("seoTitle", title);
        model.addAttribute("seoDescription", description);
        model.addAttribute("canonicalUrl", canonical);
        if (a.getCoverImage() != null && !a.getCoverImage().isEmpty()) {
            String img = a.getCoverImage();
            model.addAttribute("ogImage", img.startsWith("http") ? img : baseUrl + img);
        }

        // اسکیمای Article + BreadcrumbList (سروری، مثل الگوی دسته)
        //
        // 🔴 بردکرامب تا امروز فقط رویِ محصول و دسته بود، پس سرچ کنسول از ۲۵۵ مقاله
        // فقط ۲۵ بردکرامبِ معتبر می‌دید. حالا هر دو در یک @graph می‌آیند.
        String json = "{\"@context\":\"https://schema.org/\",\"@graph\":[" +
                "{\"@type\":\"Article\"" +
                ",\"headline\":\"" + esc(a.getTitle()) + "\"" +
                ",\"description\":\"" + esc(description) + "\"" +
                ",\"url\":\"" + esc(canonical) + "\"" +
                (a.getCoverImage() != null && !a.getCoverImage().isEmpty()
                        ? ",\"image\":\"" + esc(a.getCoverImage().startsWith("http") ? a.getCoverImage() : baseUrl + a.getCoverImage()) + "\"" : "") +
                ",\"datePublished\":\"" + a.getCreatedAt() + "\"" +
                ",\"dateModified\":\"" + a.getUpdatedAt() + "\"" +
                ",\"publisher\":{\"@type\":\"Organization\",\"name\":\"فروشگاه داده نما\",\"url\":\"" + esc(baseUrl) + "\"}}" +
                "," + articleBreadcrumbJson(a, baseUrl, canonical) + "]}";
        model.addAttribute("articleJsonLd", json);

        // مقالات هم‌خوشه (لینک‌سازی داخلی خودکار خوشه‌ی محتوایی)
        if (a.getHubSlug() != null && !a.getHubSlug().isEmpty()) {
            List<Article> related = articleService.getHubArticles(a.getHubSlug()).stream()
                    .filter(x -> !x.getId().equals(a.getId()))
                    .limit(6).toList();
            model.addAttribute("relatedArticles", related);
        }

        return "article";
    }

    /**
     * مسیرِ نان‌ریزهٔ مقاله: خانه ← بلاگ ← [خوشه] ← عنوان.
     * <p>
     * ⚠️ اگر مقاله خوشه ندارد، آن پله <b>حذف</b> می‌شود و شماره‌ها پشتِ‌سرِهم
     * می‌مانند — پلهٔ خالی یا شمارهٔ پریده، بردکرامب را در نگاهِ گوگل نامعتبر می‌کند.
     * <p>
     * ⚠️ آدرس‌ها از {@code buildBaseUrl} می‌آیند، نه دامنهٔ هاردکد: همان قاعده‌ای که
     * بقیهٔ اسکیما رعایت می‌کند تا روی لوکال و پراد هر دو درست بماند.
     */
    private String articleBreadcrumbJson(Article a, String baseUrl, String canonical) {
        StringBuilder sb = new StringBuilder("{\"@type\":\"BreadcrumbList\",\"itemListElement\":[");
        int pos = 1;
        sb.append("{\"@type\":\"ListItem\",\"position\":").append(pos++)
                .append(",\"name\":\"خانه\",\"item\":\"").append(esc(baseUrl)).append("/\"}");
        sb.append(",{\"@type\":\"ListItem\",\"position\":").append(pos++)
                .append(",\"name\":\"بلاگ\",\"item\":\"").append(esc(baseUrl)).append("/blog\"}");

        String hubSlug = a.getHubSlug();
        if (hubSlug != null && !hubSlug.isBlank()) {
            String hubName = (a.getHub() != null && !a.getHub().isBlank()) ? a.getHub() : hubSlug;
            sb.append(",{\"@type\":\"ListItem\",\"position\":").append(pos++)
                    .append(",\"name\":\"").append(esc(hubName)).append("\",\"item\":\"")
                    .append(esc(baseUrl + "/blog/hub/" + hubSlug)).append("\"}");
        }

        sb.append(",{\"@type\":\"ListItem\",\"position\":").append(pos)
                .append(",\"name\":\"").append(esc(a.getTitle())).append("\",\"item\":\"")
                .append(esc(canonical)).append("\"}]}");
        return sb.toString();
    }

    // صفحه‌ی خوشه‌ی محتوایی: /blog/hub/{hubSlug}
    @GetMapping("/blog/hub/{hubSlug}")
    public String hubPage(@PathVariable String hubSlug, Model model, HttpServletRequest request) {
        addDynamicUrls(model, request);

        List<Article> hubArticles = articleService.getHubArticles(hubSlug);
        if (hubArticles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "خوشه یافت نشد");
        }

        String hubName = hubArticles.get(0).getHub() != null ? hubArticles.get(0).getHub() : hubSlug;
        String baseUrl = buildBaseUrl(request);

        model.addAttribute("hubName", hubName);
        model.addAttribute("cards", hubArticles);
        model.addAttribute("seoTitle", hubName + " | مجموعه مقالات تخصصی – داده نما");
        model.addAttribute("seoDescription", "مجموعه کامل مقالات «" + hubName + "» — آموزش، عیب‌یابی و راهنمای تخصصی از تیم فنی داده نما");
        model.addAttribute("canonicalUrl", baseUrl + "/blog/hub/" + hubSlug);
        return "blog";
    }

    // ================= صفحه‌ی «سابقه و اعتبار» (SSR کامل) =================
    // چرا صفحه‌ی مستقل و نه فقط بلاکِ هوم: طبق فاز ۱ رودمپ، ۷۹٪ کلیک‌های سایت قدیم
    // برندی بود («داده نما») و هیچ صفحه‌ای برای گرفتنِ آن ترافیک وجود نداشت.
    //
    // 🔴 قانونِ ضد-دور-زدنِ پروژه: این صفحه هیچ راه تماس مستقیمی نمی‌دهد —
    // نه تلفن/ایمیل در متن، نه فیلد تماس در اسکیمای Organization، نه هیچ شبکه‌ی
    // اجتماعی/کانال. همه‌ی CTAها به داخل سایت‌اند.

    static final String COURSES_HUB_SLUG = "دوره‌های-آموزشی";

    // تاریخ ثبت دامنه‌ی dadehnama.com در رجیستری Verisign (راستی‌آزمایی با RDAP).
    // ⚠️ این «حضور آنلاین» است نه «تاسیس شرکت» — سند ثبت شرکت نداریم، پس متن صفحه
    // هم دقیقاً همین را می‌گوید. ۲۰۰۳-۰۴-۰۷ ≈ ۱۳۸۲.
    private static final java.time.LocalDate ONLINE_SINCE = java.time.LocalDate.of(2003, 4, 7);
    private static final String ONLINE_SINCE_JALALI = "۱۳۸۲";

    // خط‌زمانِ دوره‌ها: عمداً داده‌ی ثابتِ درون‌کد است، نه کوئری.
    // ⚠️ چرا از دیتابیس خوانده نمی‌شود: createdAtِ هر ۴۹ مقاله 2026-08-14 است
    // (تاریخ ایمپورت وردپرس)؛ تاریخ‌های واقعی برگزاری فقط داخل متنِ مقاله‌اند و
    // فیلد ساخت‌یافته ندارند. منبع: docs/reports/trust-page-articles-2026-08-19.md
    // تعدادِ کل ولی زنده از دیتابیس خوانده می‌شود (نه جمعِ دستی همین جدول).
    public static class CourseTrack {
        private final String code, title, firstHeld, lastHeld, slug;
        private final int count;

        CourseTrack(String code, String title, int count, String firstHeld, String lastHeld, String slug) {
            this.code = code; this.title = title; this.count = count;
            this.firstHeld = firstHeld; this.lastHeld = lastHeld; this.slug = slug;
        }

        public String getCode() { return code; }
        public String getTitle() { return title; }
        public int getCount() { return count; }
        public String getCountFa() { return faDigits(String.valueOf(count)); }
        public String getFirstHeld() { return firstHeld; }
        public String getLastHeld() { return lastHeld; }
        public String getSlug() { return slug; }
    }

    // مرتب بر اساس تعداد برگزاری. جمعِ ستون تعداد = ۴۹ (برابرِ شمارشِ زنده‌ی خوشه).
    // ادغام‌ها: سمینار میموسا زیر «مایکروویو و رادیوی پرظرفیت»، و «دوره تخصصی
    // وایرلس و آنالیز پیشرفته»ی بدون‌تاریخ زیر CWNA — چون هم‌خانواده‌اند.
    private static final List<CourseTrack> COURSE_TRACKS = List.of(
            new CourseTrack("MTCNA", "مقدماتی میکروتیک", 13, "شهریور ۱۳۹۴", "مرداد ۱۳۹۷", "mtcna-23-05-97"),
            new CourseTrack("MTCWE", "وایرلس میکروتیک", 8, "شهریور ۱۳۹۴", "بهمن ۱۳۹۵", "mtcwe-11-95"),
            new CourseTrack("MTCTCE", "کنترل ترافیک میکروتیک", 6, "شهریور ۱۳۹۴", "شهریور ۱۳۹۷", "mtctce-06-97"),
            new CourseTrack("VoIP", "مراکز تلفنی تحت شبکه (زایکو)", 6, "شهریور ۱۳۹۴", "تیر ۱۳۹۷", "zycoo-voip-04-97"),
            new CourseTrack("CWNA", "وایرلس تخصصی و آنالیز پیشرفته", 5, "مهر ۱۳۹۴", "مرداد ۱۳۹۶", "cwna-analysis-course-05-96"),
            new CourseTrack("مایکروویو", "ارتباطات پرظرفیت مایکروویو و رادیو", 5, "تیر ۱۳۹۵", "تیر ۱۴۰۱", "analysis-microwave-13-12-99"),
            new CourseTrack("MTCRE", "مسیریابی میکروتیک", 4, "دی ۱۳۹۴", "شهریور ۱۳۹۷", "mtcre-06-97"),
            new CourseTrack("MTCUME", "مدیریت کاربران میکروتیک", 2, "شهریور ۱۳۹۴", "دی ۱۳۹۴", "mtcume-course")
    );

    // صفحه‌ی معرفیِ WimaxNear — برندِ خودِ داده‌نما (بخشی از نوارِ هویتِ سایت).
    // 🔴 متنِ سئو عمداً «آنتن» می‌گوید و نه «رادیو»، و «مونتاژ» و نه «تولید» —
    // چون تنها سندِ موجود (کاتالوگِ خودِ شرکت + دیتاشیتِ مدل‌ها) همین را می‌گوید:
    // «مونتاژ و فروش رادیوهای حرفه‌ای WimaxNear»، و همه‌ی دیتاشیت‌ها آنتنِ دیش‌اند.
    // اگر بعداً سندی برای رادیوهایِ WimaxNear رسید، صفحه و این متن باید گسترده شود.
    @GetMapping("/wimaxnear")
    public String wimaxNearPage(Model model, HttpServletRequest request) {
        addDynamicUrls(model, request);
        String baseUrl = buildBaseUrl(request);
        model.addAttribute("seoTitle", "WimaxNear | آنتن‌های دوقطبیِ پهن‌باندِ داده‌نما");
        model.addAttribute("seoDescription",
                "خانوادهٔ آنتنِ دیشِ SP62 با برندِ WimaxNear داده‌نما — دوقطبی، ۴٫۸ تا ۶٫۲ گیگاهرتز، بهرهٔ ۲۵ تا ۳۴ dBi، سازگار با ETSI EN 302.326-3، همراه با نمودارهای اندازه‌گیریِ آزمایشگاه.");
        model.addAttribute("canonicalUrl", baseUrl + "/wimaxnear");
        return "wimaxnear";
    }

    // صفحه‌ی آموزش — آرشیوِ تصویریِ کلاس‌ها و دوره‌هایِ برگزارشده (سابقه، نه اعلانِ دورهٔ فعال)
    @GetMapping("/learn")
    public String learnPage(Model model, HttpServletRequest request) {
        addDynamicUrls(model, request);
        String baseUrl = buildBaseUrl(request);
        model.addAttribute("seoTitle", "آموزش | آرشیوِ دوره‌ها و کلاس‌های برگزارشده‌ی داده‌نما");
        model.addAttribute("seoDescription",
                "آرشیوِ تصویریِ دوره‌ها، کلاس‌ها و سمینارهایِ برگزارشده‌ی داده‌نما در حوزه‌ی شبکه و ارتباطاتِ بی‌سیم.");
        model.addAttribute("canonicalUrl", baseUrl + "/learn");
        model.addAttribute("educationArchiveItems", educationArchiveService.getActive());
        // دوره‌های زنده بالا، آرشیو پایین — طبقِ docs/prompt-tech-chat-course-system.md
        model.addAttribute("courses", courseService.getAllForLearnPage());
        return "learn";
    }

    // ابزارِ «محاسبه لینک وایرلس» — اپِ داده‌لینک، کامل در خودِ سایت.
    // 🔴 نشانی عمداً همان نشانیِ ابزارِ سایتِ قدیمی است (۳۴۲ کلیک و جایگاهِ ۵ در ۱۶ ماه).
    // موتورِ محاسبه در static/tools/dadehlink/engine/ بایت‌به‌بایت از اپ کپی شده و این‌جا
    // ویرایش نمی‌شود — شرحش در VERSION همان پوشه و docs/prompt-tech-chat-dadehlink-in-shop.md.
    @GetMapping("/support/link-cal")
    public String linkCalPage(Model model, HttpServletRequest request) {
        addDynamicUrls(model, request);
        String baseUrl = buildBaseUrl(request);
        String canonical = baseUrl + "/support/link-cal";
        String title = "محاسبه لینک وایرلس — ابزار رایگان بودجه لینک رادیویی | داده نما";
        String description = "محاسبهٔ رایگانِ بودجهٔ لینکِ رادیویی: سیگنالِ دریافتی، حاشیهٔ تضعیف، ناحیهٔ فرنل و افتِ باران برای شهرهای ایران. بدونِ ثبت‌نام، روی موبایل و دسکتاپ.";
        model.addAttribute("seoTitle", title);
        model.addAttribute("seoDescription", description);
        model.addAttribute("canonicalUrl", canonical);
        model.addAttribute("linkCalJsonLd", buildLinkCalJsonLd(baseUrl, canonical, description));
        return "link-cal";
    }

    private String buildLinkCalJsonLd(String baseUrl, String canonical, String description) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"@context\":\"https://schema.org/\",\"@graph\":[");
        sb.append("{\"@type\":\"WebApplication\",\"name\":\"محاسبه لینک وایرلس\"")
                .append(",\"url\":\"").append(esc(canonical)).append("\"")
                .append(",\"description\":\"").append(esc(description)).append("\"")
                .append(",\"applicationCategory\":\"UtilitiesApplication\"")
                .append(",\"operatingSystem\":\"Any\"")
                .append(",\"inLanguage\":\"fa\"")
                .append(",\"offers\":{\"@type\":\"Offer\",\"price\":0,\"priceCurrency\":\"IRR\"}}");
        sb.append(",{\"@type\":\"BreadcrumbList\",\"itemListElement\":[")
                .append("{\"@type\":\"ListItem\",\"position\":1,\"name\":\"خانه\",\"item\":\"")
                .append(esc(baseUrl)).append("/\"},")
                .append("{\"@type\":\"ListItem\",\"position\":2,\"name\":\"محاسبه لینک وایرلس\",\"item\":\"")
                .append(esc(canonical)).append("\"}]}");
        sb.append("]}");
        return sb.toString();
    }

    @GetMapping("/about")
    public String aboutPage(Model model, HttpServletRequest request) {
        addDynamicUrls(model, request);

        String baseUrl = buildBaseUrl(request);
        String canonical = baseUrl + "/about";

        // یک کوئری، دو کار: شمارشِ زنده‌ی دوره‌ها + اعتبارسنجی اسلاگ‌های جدول بالا
        List<Article> courseArticles = articleService.getHubArticles(COURSES_HUB_SLUG);
        java.util.Set<String> liveSlugs = new java.util.HashSet<>();
        for (Article a : courseArticles) {
            if (a.getSlug() != null && !a.getSlug().isEmpty()) liveSlugs.add(a.getSlug());
        }
        // اگر مقاله‌ای بعداً حذف شود یا اسلاگش عوض شود، آن سطر از جدول می‌افتد —
        // بهتر از این‌که لینکِ ۴۰۴ روی صفحه‌ی اعتبار بماند.
        List<CourseTrack> tracks = COURSE_TRACKS.stream()
                .filter(t -> liveSlugs.contains(t.getSlug()))
                .toList();

        int years = java.time.Period.between(ONLINE_SINCE, java.time.LocalDate.now()).getYears();
        String yearsFa = faDigits(String.valueOf(years));
        String courseCountFa = faDigits(String.valueOf(courseArticles.size()));

        model.addAttribute("courseTracks", tracks);
        model.addAttribute("courseCountFa", courseCountFa);
        model.addAttribute("trackCountFa", faDigits(String.valueOf(tracks.size())));
        model.addAttribute("onlineYearsFa", yearsFa);
        model.addAttribute("onlineSinceFa", ONLINE_SINCE_JALALI);
        model.addAttribute("coursesHubUrl", baseUrl + "/blog/hub/" + encodePathSegment(COURSES_HUB_SLUG));

        String title = "سابقه و اعتبار داده نما | بیش از " + yearsFa + " سال در شبکه و میکروتیک";
        String description = "سابقه‌ی داده نما: حضور آنلاین از سال " + ONLINE_SINCE_JALALI + " و " + courseCountFa
                + " دوره‌ی رسمی برگزارشده‌ی میکروتیک (MTCNA، MTCWE، MTCRE، MTCTCE، MTCUME)، "
                + "وایرلس CWNA، مایکروویو پرظرفیت و VoIP در بازه‌ی ۱۳۹۴ تا ۱۴۰۱.";

        model.addAttribute("seoTitle", title);
        model.addAttribute("seoDescription", description);
        model.addAttribute("canonicalUrl", canonical);
        model.addAttribute("aboutJsonLd", buildAboutJsonLd(baseUrl, canonical, description));

        return "about";
    }

    // اسکیمای صفحه‌ی اعتبار (سروری، مثل الگوی categoryJsonLd — گاتچای Thymeleaf
    // در ld+json قبلاً در این پروژه دردسر ساخته بود).
    // ⚠️ عمداً بدون telephone/email/sameAs (قانون ضد-دور-زدن) و بدون foundingDate
    // (تاریخِ ثبتِ دامنه سندِ تاسیسِ شرکت نیست).
    private String buildAboutJsonLd(String baseUrl, String canonical, String description) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"@context\":\"https://schema.org/\",\"@graph\":[");

        sb.append(buildOrganizationJsonLd(baseUrl, description));

        sb.append(",{\"@type\":\"AboutPage\",\"name\":\"سابقه و اعتبار داده نما\"")
                .append(",\"url\":\"").append(esc(canonical)).append("\"")
                .append(",\"description\":\"").append(esc(description)).append("\"}");

        sb.append(",{\"@type\":\"BreadcrumbList\",\"itemListElement\":[")
                .append("{\"@type\":\"ListItem\",\"position\":1,\"name\":\"خانه\",\"item\":\"")
                .append(esc(baseUrl)).append("/\"},")
                .append("{\"@type\":\"ListItem\",\"position\":2,\"name\":\"سابقه و اعتبار\",\"item\":\"")
                .append(esc(canonical)).append("\"}]}");

        sb.append("]}");
        return sb.toString();
    }

    /**
     * بلاکِ Organization به‌تنهایی — هم رویِ {@code /about} (داخلِ @graph) و هم رویِ
     * ریشهٔ سایت {@code /} (که خودش جایگاهِ درست‌ترِ این اسکیماست) استفاده می‌شود.
     * ⚠️ عمداً بدون telephone/email/sameAs (قانون ضد-دور-زدن).
     */
    private String buildOrganizationJsonLd(String baseUrl, String description) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"@type\":\"Organization\",\"name\":\"داده نما\"")
                .append(",\"alternateName\":\"شرکت ارتباطات شبکه داده نما\"")
                .append(",\"url\":\"").append(esc(baseUrl)).append("/\"")
                .append(",\"logo\":\"").append(esc(baseUrl)).append("/img/logo.png\"")
                .append(",\"description\":\"").append(esc(description)).append("\"}");
        return sb.toString();
    }

    /** سندِ کاملِ مستقلِ JSON-LD (با {@code @context}) — برای ریشهٔ سایت که فقط همین یک اسکیما را دارد. */
    private String buildStandaloneOrganizationJsonLd(String baseUrl, String description) {
        return "{\"@context\":\"https://schema.org/\",\"@graph\":[" + buildOrganizationJsonLd(baseUrl, description) + "]}";
    }

    // ارقام لاتین → فارسی (عددهای داخل متنِ فارسیِ صفحه باید فارسی باشند)
    static String faDigits(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            sb.append(c >= '0' && c <= '9' ? (char) ('۰' + (c - '0')) : c);
        }
        return sb.toString();
    }

    // percent-encode یک سگمنتِ مسیر (اسلاگ فارسی) — URLEncoder فاصله را + می‌کند
    private static String encodePathSegment(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }

    // متد اختصاصی برای ساخت آدرس‌های داینامیک بدون هاردکد کردن localhost
    private void addDynamicUrls(Model model, HttpServletRequest request) {
        String baseUrl = buildBaseUrl(request);
        model.addAttribute("baseUrl", baseUrl);
        model.addAttribute("currentUrl", request.getRequestURL().toString());
        // تصویر پیش‌فرض سئو؛ در صفحه‌ی محصول با تصویر واقعی بازنویسی می‌شود
        model.addAttribute("ogImage", baseUrl + "/img/logo.png");

        // آنالیتیکس (GA4 + تایید Search Console) — فقط وقتی مقدار دارند رندر می‌شوند
        model.addAttribute("ga4Id", ga4Id != null ? ga4Id.trim() : "");
        model.addAttribute("gscToken", gscToken != null ? gscToken.trim() : "");

        // لینک‌های فوتر به صفحات دسته (لینک داخلی واقعی برای خزنده‌ها در همه صفحات)
        List<Category> footerCategories = categoryRepo.findAll().stream()
                .filter(c -> c.getType() == null || !"WAREHOUSE".equalsIgnoreCase(c.getType()))
                .filter(c -> c.getName() != null && !c.getName().isBlank())
                .limit(30)
                .toList();
        model.addAttribute("footerCategories", footerCategories);
    }

    private String buildBaseUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() +
                (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort());
    }

    // robots.txt داینامیک که به sitemap اشاره می‌کند
    @GetMapping(value = "/robots.txt", produces = "text/plain")
    @org.springframework.web.bind.annotation.ResponseBody
    public String robots(HttpServletRequest request) {
        String baseUrl = buildBaseUrl(request);
        return "User-agent: *\n" +
                "Allow: /\n" +
                "Disallow: /Admin.html\n" +
                "Disallow: /AdminLogin.html\n" +
                "Disallow: /api/\n" +
                // لینک‌های کوتاهِ کارزار نباید ایندکس شوند: خودشان محتوا ندارند و اگر
                // ایندکس شوند، نسخهٔ برچسب‌خوردهٔ صفحهٔ مقصد را به جانِ سئو می‌اندازند.
                // هدرِ X-Robots-Tag روی خودِ پاسخ هم هست؛ این لایهٔ دوم است.
                "Disallow: /l/\n" +
                "Sitemap: " + baseUrl + "/sitemap.xml\n";
    }

    // sitemap.xml روی روت (استاندارد گوگل)؛ lastmod امن در برابر null
    @GetMapping(value = "/sitemap.xml", produces = "application/xml")
    @org.springframework.web.bind.annotation.ResponseBody
    public String sitemap(HttpServletRequest request) {
        String baseUrl = buildBaseUrl(request);
        List<Product> products = productRepo.findAll();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
        xml.append("<url><loc>").append(baseUrl).append("/</loc><priority>1.0</priority></url>");

        // فروشگاه — ریشه‌ی زیردرختِ محصولات/دسته‌ها (تصمیمِ معماریِ داده‌نما/فروشگاه/آموزش)
        xml.append("<url><loc>").append(baseUrl).append("/shop</loc><priority>0.95</priority></url>");

        // صفحه‌ی سابقه و اعتبار (هدفِ جست‌وجوی برندی — ۷۹٪ کلیک‌های سایت قدیم)
        xml.append("<url><loc>").append(baseUrl).append("/about</loc><priority>0.8</priority></url>");

        // صفحه‌ی WimaxNear
        xml.append("<url><loc>").append(baseUrl).append("/wimaxnear</loc><priority>0.6</priority></url>");

        // صفحه‌ی آموزش
        xml.append("<url><loc>").append(baseUrl).append("/learn</loc><priority>0.6</priority></url>");

        // ابزارِ محاسبهٔ لینک
        xml.append("<url><loc>").append(baseUrl).append("/support/link-cal</loc><priority>0.7</priority></url>");

        // بلاگ و مقالات منتشرشده
        xml.append("<url><loc>").append(baseUrl).append("/blog</loc><priority>0.7</priority></url>");
        // صفحات خوشه‌های محتوایی (یکتا)
        java.util.Set<String> hubSlugs = new java.util.HashSet<>();
        for (Article a : articleService.getAllPublished()) {
            if (a.getHubSlug() != null && !a.getHubSlug().isEmpty() && hubSlugs.add(a.getHubSlug())) {
                xml.append("<url><loc>").append(baseUrl).append("/blog/hub/").append(a.getHubSlug())
                        .append("</loc><priority>0.7</priority></url>");
            }
        }
        for (Article a : articleService.getAllPublished()) {
            String aSlug = (a.getSlug() != null && !a.getSlug().isEmpty()) ? a.getSlug() : a.getId();
            Instant amod = a.getUpdatedAt() != null ? a.getUpdatedAt()
                    : (a.getCreatedAt() != null ? a.getCreatedAt() : Instant.now());
            xml.append("<url><loc>").append(baseUrl).append("/blog/").append(aSlug).append("</loc>");
            xml.append("<lastmod>").append(amod.toString(), 0, 10).append("</lastmod>");
            xml.append("<priority>0.7</priority></url>");
        }

        // صفحات دسته‌بندی (فقط دسته‌های فروشگاه آنلاین، نه انبار)
        for (Category c : categoryRepo.findAll()) {
            if (c.getType() != null && "WAREHOUSE".equalsIgnoreCase(c.getType())) continue;
            String cSlug = (c.getSlug() != null && !c.getSlug().isEmpty()) ? c.getSlug() : c.getId();
            xml.append("<url>");
            xml.append("<loc>").append(baseUrl).append("/shop/category/").append(cSlug).append("</loc>");
            xml.append("<priority>0.9</priority>");
            xml.append("</url>");
        }

        for (Product p : products) {
            // آدرسی که ۳۰۱ می‌دهد در نقشهٔ سایت جایی ندارد — گوگل آن را خطا می‌شمرد.
            // متوقف‌شدهٔ بی‌جایگزین می‌ماند، چون صفحه‌اش زنده است و ۲۰۰ می‌دهد.
            if (liveReplacementOf(p).isPresent()) continue;
            // آدرسِ هیبریدِ کاملاً percent-encode شده (resolver + دُمِ فارسی)
            String loc = baseUrl + hybridPathEncoded(p);
            Instant mod = p.getUpdatedAt() != null ? p.getUpdatedAt()
                    : (p.getCreatedAt() != null ? p.getCreatedAt() : Instant.now());
            xml.append("<url>");
            xml.append("<loc>").append(loc).append("</loc>");
            xml.append("<lastmod>").append(mod.toString(), 0, 10).append("</lastmod>");
            xml.append("<priority>0.8</priority>");
            xml.append("</url>");
        }
        xml.append("</urlset>");
        return xml.toString();
    }

    @GetMapping("/profile") // یا هر آدرس دلخواهی مثل /customer-panel
    public String customerPanel(Model model, HttpServletRequest request) {
        // همان کدی که برای CL.html زدیم را اینجا هم می‌زنیم تا آدرس‌ها داینامیک بمانند
        addDynamicUrls(model, request);
        return "customerPanel"; // نام فایل HTML بدون پسوند .html
    }
}