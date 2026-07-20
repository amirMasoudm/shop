package org.example.shop1.controller;

import jakarta.servlet.http.HttpServletRequest; // اضافه شد
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.entity.Category;
import org.example.shop1.model.reposritory.ProductRepository;
import org.example.shop1.model.reposritory.CategoryRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Controller
public class StoreWebController {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;

    public StoreWebController(ProductRepository productRepo, CategoryRepository categoryRepo) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
    }

    @GetMapping("/")
    public String homePage(Model model, HttpServletRequest request) {
        // متد کمکی برای اضافه کردن آدرس‌های زنده (جلوگیری از خطای ۵۰۰)
        addDynamicUrls(model, request);

        model.addAttribute("seoTitle", "داده نما | اتصال آسان است");
        model.addAttribute("seoDescription", "فروشگاه اینترنتی یاس، عرضه کننده بهترین محصولات با گارانتی معتبر و ارسال فوری");
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

            String catName = "داده نما";
            if (p.getCategoryId() != null) {
                catName = categoryRepo.findById(p.getCategoryId())
                        .map(Category::getName)
                        .orElse("داده نما");
            }
            model.addAttribute("categoryName", catName);
        } else {
            return "redirect:/";
        }
        return "CL";
    }

    // متد اختصاصی برای ساخت آدرس‌های داینامیک بدون هاردکد کردن localhost
    private void addDynamicUrls(Model model, HttpServletRequest request) {
        String baseUrl = buildBaseUrl(request);
        model.addAttribute("baseUrl", baseUrl);
        model.addAttribute("currentUrl", request.getRequestURL().toString());
        // تصویر پیش‌فرض سئو؛ در صفحه‌ی محصول با تصویر واقعی بازنویسی می‌شود
        model.addAttribute("ogImage", baseUrl + "/logo.png");
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