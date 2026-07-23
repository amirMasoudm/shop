package org.example.shop1.controller;

import jakarta.servlet.http.HttpServletRequest; // اضافه شد
import org.example.shop1.model.entity.Article;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.entity.Category;
import org.example.shop1.model.reposritory.ProductRepository;
import org.example.shop1.model.reposritory.CategoryRepository;
import org.example.shop1.model.service.ArticleService;
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

    // فاز ۰ رودمپ: آنالیتیکس — خالی بودن یعنی تگ رندر نمی‌شود
    @org.springframework.beans.factory.annotation.Value("${analytics.ga4.measurement-id:}")
    private String ga4Id;

    @org.springframework.beans.factory.annotation.Value("${analytics.gsc.verification:}")
    private String gscToken;

    public StoreWebController(ProductRepository productRepo, CategoryRepository categoryRepo,
                              CategoryService categoryService, ArticleService articleService) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.categoryService = categoryService;
        this.articleService = articleService;
    }

    @GetMapping("/")
    public String homePage(Model model, HttpServletRequest request) {
        // متد کمکی برای اضافه کردن آدرس‌های زنده (جلوگیری از خطای ۵۰۰)
        addDynamicUrls(model, request);

        model.addAttribute("seoTitle", "داده نما | اتصال آسان است");
        model.addAttribute("seoDescription", "فروشگاه اینترنتی یاس، عرضه کننده بهترین محصولات با گارانتی معتبر و ارسال فوری");
        model.addAttribute("canonicalUrl", buildBaseUrl(request) + "/");
        return "CL";
    }

    @GetMapping("/product/{slugOrId}")
    public String productPage(@PathVariable String slugOrId, Model model, HttpServletRequest request) {
        // متد کمکی برای اضافه کردن آدرس‌های زنده
        addDynamicUrls(model, request);

        Optional<Product> productOpt = productRepo.findBySlug(slugOrId);
        if (productOpt.isEmpty()) {
            productOpt = productRepo.findById(slugOrId);
        }

        if (productOpt.isPresent()) {
            Product p = productOpt.get();
            model.addAttribute("p", p);
            model.addAttribute("seoTitle", p.getSeoTitle() != null ? p.getSeoTitle() : p.getName());
            model.addAttribute("seoDescription", p.getSeoDescription() != null ? p.getSeoDescription() : "خرید آنلاین محصول " + p.getName());

            // آدرس مطلق تصویر برای og:image و JSON-LD (تصاویر در دیتابیس نسبی‌اند: /uploads/..)
            String baseUrl = buildBaseUrl(request);
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

            String slug = (p.getSlug() != null && !p.getSlug().isEmpty()) ? p.getSlug() : p.getId();
            model.addAttribute("productSlug", slug);

            // canonical همیشه نسخه‌ی اسلاگ است (جلوگیری از ایندکس دوگانه‌ی /product/{id} و /product/{slug})
            model.addAttribute("canonicalUrl", baseUrl + "/product/" + slug);

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
        } else {
            // ۴۰۴ واقعی به‌جای ریدایرکت (soft 404 برای گوگل مضر است)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "محصول یافت نشد");
        }
        return "CL";
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
                String pSlug = (p.getSlug() != null && !p.getSlug().isEmpty()) ? p.getSlug() : p.getId();
                if (i > 0) sb.append(",");
                sb.append("{\"@type\":\"ListItem\",\"position\":").append(pos++)
                        .append(",\"url\":\"").append(esc(baseUrl + "/product/" + pSlug)).append("\"}");
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
            String slug = (p.getSlug() != null && !p.getSlug().isEmpty()) ? p.getSlug() : p.getId();
            Instant mod = p.getUpdatedAt() != null ? p.getUpdatedAt()
                    : (p.getCreatedAt() != null ? p.getCreatedAt() : Instant.now());
            xml.append("<url>");
            xml.append("<loc>").append(baseUrl).append("/product/").append(slug).append("</loc>");
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