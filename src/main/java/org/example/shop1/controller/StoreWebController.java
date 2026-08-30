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
    private final org.example.shop1.model.service.ProductRedirectService productRedirectService;

    // فاز ۰ رودمپ: آنالیتیکس — خالی بودن یعنی تگ رندر نمی‌شود
    @org.springframework.beans.factory.annotation.Value("${analytics.ga4.measurement-id:}")
    private String ga4Id;

    @org.springframework.beans.factory.annotation.Value("${analytics.gsc.verification:}")
    private String gscToken;

    public StoreWebController(ProductRepository productRepo, CategoryRepository categoryRepo,
                              CategoryService categoryService, ArticleService articleService,
                              BannerService bannerService,
                              org.example.shop1.model.service.ProductRedirectService productRedirectService) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.categoryService = categoryService;
        this.articleService = articleService;
        this.bannerService = bannerService;
        this.productRedirectService = productRedirectService;
    }

    @GetMapping("/")
    public String homePage(Model model, HttpServletRequest request) {
        // متد کمکی برای اضافه کردن آدرس‌های زنده (جلوگیری از خطای ۵۰۰)
        addDynamicUrls(model, request);

        model.addAttribute("seoTitle", "داده نما | اتصال آسان است");
        model.addAttribute("seoDescription", "فروشگاه اینترنتی یاس، عرضه کننده بهترین محصولات با گارانتی معتبر و ارسال فوری");
        model.addAttribute("canonicalUrl", buildBaseUrl(request) + "/");

        // اسلایدرِ بنر — بالاترینِ عنصرِ صفحه (LCP)؛ لینک‌ها همین‌جا (نه در زمانِ
        // ذخیره‌ی بنر) resolve می‌شوند تا تغییرِ بعدیِ نامِ محصول/دسته/مقاله لینک را نشکند.
        model.addAttribute("banners", bannerService.getHeroBannersResolved());

        // بلاکِ خلاصه‌ی «سابقه و اعتبار» (SSR؛ لینک به /about). شمارشِ سبک است تا
        // صفحه‌ی اصلی برای یک عدد، ۴۹ سندِ کامل نخواند.
        model.addAttribute("trustCourseCountFa",
                faDigits(String.valueOf(articleService.countHubArticles(COURSES_HUB_SLUG))));
        model.addAttribute("trustSinceFa", ONLINE_SINCE_JALALI);
        model.addAttribute("trustYearsFa", faDigits(String.valueOf(
                java.time.Period.between(ONLINE_SINCE, java.time.LocalDate.now()).getYears())));
        return "CL";
    }

    // آدرسِ قدیمیِ تک‌بخشی — برای سازگاریِ عقب همچنان ۲۰۰ می‌دهد؛ canonical به فرمِ هیبرید هدایت می‌کند
    @GetMapping("/product/{slugOrId}")
    public Object productPage(@PathVariable String slugOrId, Model model, HttpServletRequest request) {
        addDynamicUrls(model, request);
        Optional<Product> found = resolveProduct(slugOrId);
        if (found.isEmpty()) {
            // محصول نیست: شاید اسلاگش بعدِ ادغام ریدایرکت شده باشد (۳۰۱ به‌جایِ ۴۰۴)
            Object redirect = redirectOrNull(slugOrId, request);
            if (redirect != null) return redirect;
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "محصول یافت نشد");
        }
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

    // آدرسِ هیبریدِ جدید: /product/{resolver}/{persianTail}
    // resolver محصول را قطعی resolve می‌کند؛ دُم فقط تزئینی/سئو است و برای lookup نادیده گرفته می‌شود.
    @GetMapping("/product/{resolver}/{persianTail}")
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
    }

    // ================= صفحه‌ی سئوی دسته‌بندی =================
    @GetMapping("/category/{slugOrId}")
    public String categoryPage(@PathVariable String slugOrId, Model model, HttpServletRequest request) {
        addDynamicUrls(model, request);

        Category cat = categoryService.findBySlugOrId(slugOrId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "دسته یافت نشد"));

        String baseUrl = buildBaseUrl(request);
        String slug = (cat.getSlug() != null && !cat.getSlug().isEmpty()) ? cat.getSlug() : cat.getId();
        String canonical = baseUrl + "/category/" + slug;

        String title = (cat.getSeoTitle() != null && !cat.getSeoTitle().isEmpty())
                ? cat.getSeoTitle()
                : "خرید " + cat.getName() + " | فروشگاه داده نما";
        String description = (cat.getSeoDescription() != null && !cat.getSeoDescription().isEmpty())
                ? cat.getSeoDescription()
                : "خرید انواع " + cat.getName() + " با بهترین قیمت و گارانتی معتبر از فروشگاه داده نما";

        List<Product> catProducts = categoryService.getProductsInSubtree(cat.getId(), 60);

        model.addAttribute("cat", cat);
        model.addAttribute("catProducts", catProducts);
        model.addAttribute("seoTitle", title);
        model.addAttribute("seoDescription", description);
        model.addAttribute("canonicalUrl", canonical);
        model.addAttribute("categoryJsonLd", buildCategoryJsonLd(cat, catProducts, baseUrl, canonical, description));

        return "CL";
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
                .append("{\"@type\":\"ListItem\",\"position\":1,\"name\":\"خانه\",\"item\":\"")
                .append(esc(baseUrl)).append("/\"},")
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
    public String articlePage(@PathVariable String slugOrId, Model model, HttpServletRequest request) {
        addDynamicUrls(model, request);

        Article a = articleService.getPublishedBySlugOrId(slugOrId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "مقاله یافت نشد"));

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

        // اسکیمای Article (سروری، مثل الگوی دسته)
        String json = "{\"@context\":\"https://schema.org/\",\"@type\":\"Article\"" +
                ",\"headline\":\"" + esc(a.getTitle()) + "\"" +
                ",\"description\":\"" + esc(description) + "\"" +
                ",\"url\":\"" + esc(canonical) + "\"" +
                (a.getCoverImage() != null && !a.getCoverImage().isEmpty()
                        ? ",\"image\":\"" + esc(a.getCoverImage().startsWith("http") ? a.getCoverImage() : baseUrl + a.getCoverImage()) + "\"" : "") +
                ",\"datePublished\":\"" + a.getCreatedAt() + "\"" +
                ",\"dateModified\":\"" + a.getUpdatedAt() + "\"" +
                ",\"publisher\":{\"@type\":\"Organization\",\"name\":\"فروشگاه داده نما\",\"url\":\"" + esc(baseUrl) + "\"}}";
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

        sb.append("{\"@type\":\"Organization\",\"name\":\"داده نما\"")
                .append(",\"alternateName\":\"فروشگاه داده نما\"")
                .append(",\"url\":\"").append(esc(baseUrl)).append("/\"")
                .append(",\"logo\":\"").append(esc(baseUrl)).append("/logo.png\"")
                .append(",\"description\":\"").append(esc(description)).append("\"}");

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
        model.addAttribute("ogImage", baseUrl + "/logo.png");

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

        // صفحه‌ی سابقه و اعتبار (هدفِ جست‌وجوی برندی — ۷۹٪ کلیک‌های سایت قدیم)
        xml.append("<url><loc>").append(baseUrl).append("/about</loc><priority>0.8</priority></url>");

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
            xml.append("<loc>").append(baseUrl).append("/category/").append(cSlug).append("</loc>");
            xml.append("<priority>0.9</priority>");
            xml.append("</url>");
        }

        for (Product p : products) {
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