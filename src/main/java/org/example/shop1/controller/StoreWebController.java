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

        model.addAttribute("seoTitle", "فروشگاه یاس | خرید آنلاین با بهترین قیمت");
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

            String slug = (p.getSlug() != null && !p.getSlug().isEmpty()) ? p.getSlug() : p.getId();
            model.addAttribute("productSlug", slug);

            String catName = "فروشگاه یاس";
            if (p.getCategoryId() != null) {
                catName = categoryRepo.findById(p.getCategoryId())
                        .map(Category::getName)
                        .orElse("فروشگاه یاس");
            }
            model.addAttribute("categoryName", catName);
        } else {
            return "redirect:/";
        }
        return "CL";
    }

    // متد اختصاصی برای ساخت آدرس‌های داینامیک بدون هاردکد کردن localhost
    private void addDynamicUrls(Model model, HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() +
                (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort());

        model.addAttribute("baseUrl", baseUrl);
        model.addAttribute("currentUrl", request.getRequestURL().toString());
    }

    @GetMapping("/profile") // یا هر آدرس دلخواهی مثل /customer-panel
    public String customerPanel(Model model, HttpServletRequest request) {
        // همان کدی که برای CL.html زدیم را اینجا هم می‌زنیم تا آدرس‌ها داینامیک بمانند
        addDynamicUrls(model, request);
        return "customerPanel"; // نام فایل HTML بدون پسوند .html
    }
}