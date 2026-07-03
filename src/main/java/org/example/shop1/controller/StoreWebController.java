package org.example.shop1.controller;

import org.example.shop1.model.entity.Product;
import org.example.shop1.model.entity.Category; // اضافه شد
import org.example.shop1.model.reposritory.ProductRepository;
import org.example.shop1.model.reposritory.CategoryRepository; // اضافه شد
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Controller
public class StoreWebController {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo; // اضافه شد

    public StoreWebController(ProductRepository productRepo, CategoryRepository categoryRepo) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
    }

    @GetMapping("/")
    public String homePage(Model model) {
        model.addAttribute("seoTitle", "فروشگاه یاس | خرید آنلاین با بهترین قیمت");
        model.addAttribute("seoDescription", "فروشگاه اینترنتی یاس، عرضه کننده بهترین محصولات با گارانتی معتبر و ارسال فوری");
        return "CL";
    }

    @GetMapping("/product/{slugOrId}")
    public String productPage(@PathVariable String slugOrId, Model model) {
        Optional<Product> productOpt = productRepo.findBySlug(slugOrId);
        if (productOpt.isEmpty()) {
            productOpt = productRepo.findById(slugOrId);
        }

        if (productOpt.isPresent()) {
            Product p = productOpt.get();
            model.addAttribute("p", p);
            model.addAttribute("seoTitle", p.getSeoTitle() != null ? p.getSeoTitle() : p.getName());
            model.addAttribute("seoDescription", p.getSeoDescription() != null ? p.getSeoDescription() : "خرید آنلاین محصول");

            // تضمین وجود اسلاگ
            String slug = (p.getSlug() != null && !p.getSlug().isEmpty()) ? p.getSlug() : p.getId();
            model.addAttribute("productSlug", slug);

            // تضمین وجود نام دسته‌بندی (اگر تهی بود مقدار پیش‌فرض می‌گذارد)
            String catName = "فروشگاه یاس";
            if (p.getCategoryId() != null) {
                catName = categoryRepo.findById(p.getCategoryId())
                        .map(Category::getName)
                        .orElse("فروشگاه یاس");
            }
            model.addAttribute("categoryName", catName);

        } else {
            return "redirect:/"; // اگر محصول نبود برود صفحه اصلی
        }
        return "CL";
    }
}