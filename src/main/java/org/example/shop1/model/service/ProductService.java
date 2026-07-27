package org.example.shop1.model.service;

import org.example.shop1.model.dto.ProductRequest;
import org.example.shop1.model.entity.Category;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.reposritory.CategoryRepository;
import org.example.shop1.model.reposritory.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;

    public ProductService(ProductRepository productRepo, CategoryRepository categoryRepo) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
    }

    // متد کمکی برای اعمال تخفیف
    private void applyDiscount(Product product, Integer discountPercent) {
        if (discountPercent != null && discountPercent > 0 && product.getPrice() != null) {
            BigDecimal price = product.getPrice();
            BigDecimal discountFactor = BigDecimal.valueOf(100 - discountPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            product.setDiscountedPrice(price.multiply(discountFactor).setScale(2, RoundingMode.HALF_UP));
            product.setDiscountPercent(discountPercent);
        } else {
            product.setDiscountedPrice(product.getPrice());
            product.setDiscountPercent(0);
        }
    }

    // متد ایجاد محصول - با کنترل یونیک بودن ID و پر کردن فیلدهای جدید
    public Product createProduct(ProductRequest request) {

        // ۱. کنترل شناسه محصول
        if (request.getId() != null && !request.getId().trim().isEmpty()) {
            if (productRepo.existsById(request.getId())) {
                throw new RuntimeException("Product ID '" + request.getId() + "' already exists. Please choose a unique ID.");
            }
        }

        // ۲. بررسی وجود دسته‌بندی
        Category category = categoryRepo.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));

        Product product = new Product();
        product.setId(request.getId()); // تنظیم ID (اگر null باشد، Mongo آن را تولید می‌کند)
        product.setName(request.getName());
        product.setCategoryId(category.getId());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setDescription(request.getDescription());
        product.setImages(request.getImages());
        if (request.getImageAlts() != null) product.setImageAlts(request.getImageAlts()); // فقط اگر ارسال شد (بدون پاک‌کردنِ ناخواسته)
        product.setSpecifications(request.getSpecifications());
        product.setBasePrice(request.getBasePrice()); // مقداردهی قیمت پایه
        product.setUpdatedAt(Instant.now()); // زمان ساخت همان زمان بروزرسانی اولیه است
        // ۳. تنظیم فیلدهای سورتینگ اولیه
        product.setCreatedAt(Instant.now());
        product.setSalesCount(0L);
        product.setReviewCount(0L);
        product.setAverageRating(0.0);
        product.setWarehouseDescription(request.getWarehouseDescription());

        // ---> کدهای جدید سئو <---
        product.setSeoTitle(request.getSeoTitle());
        product.setSeoDescription(request.getSeoDescription());
        // اسلاگ تمیز و یکتا (اگر ورودی خالی بود از نام محصول ساخته می‌شود)
        String slugBase = (request.getSlug() == null || request.getSlug().trim().isEmpty())
                ? request.getName() : request.getSlug();
        product.setSlug(generateUniqueSlug(slugBase, null));
        // ---> پایان کدهای جدید سئو <---

        applyRichContent(product, request);

        product.setUnit(request.getUnit());
        product.setPackQuantity(request.getPackQuantity());

        product.setOnlinePrice(request.getOnlinePrice());


        // ۴. اعمال تخفیف و ذخیره
        applyDiscount(product, request.getDiscountPercent());
// داخل متد createProduct و updateProduct:
        product.setWeight(request.getWeight());
        product.setLength(request.getLength());
        product.setWidth(request.getWidth());
        product.setHeight(request.getHeight());

        product.setWarehouseCategoryId(request.getWarehouseCategoryId());
        return productRepo.save(product);
    }

    // متد به‌روزرسانی محصول
    public Product updateProduct(String id, ProductRequest request) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // **نکته:** در آپدیت، ID اصلی نباید تغییر کند.
        // تغییرات را فقط روی سایر فیلدها اعمال می‌کنیم.

        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setDescription(request.getDescription());
        product.setBasePrice(request.getBasePrice()); // آپدیت قیمت پایه
        product.setUpdatedAt(Instant.now()); // ثبت زمان آپدیت
        product.setWarehouseDescription(request.getWarehouseDescription());
        product.setUnit(request.getUnit());
        product.setPackQuantity(request.getPackQuantity());
        product.setOnlinePrice(request.getOnlinePrice());

        product.setWeight(request.getWeight());
        product.setLength(request.getLength());
        product.setWidth(request.getWidth());
        product.setHeight(request.getHeight());

        if (!product.getCategoryId().equals(request.getCategoryId())) {
            Category category = categoryRepo.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategoryId(category.getId());
        }

        product.setImages(request.getImages());
        if (request.getImageAlts() != null) product.setImageAlts(request.getImageAlts()); // فقط اگر ارسال شد (بدون پاک‌کردنِ ناخواسته)
        product.setSpecifications(request.getSpecifications());

        // اعمال مجدد تخفیف با درصد جدید
        applyDiscount(product, request.getDiscountPercent());
// داخل متد createProduct و updateProduct:
        product.setWarehouseCategoryId(request.getWarehouseCategoryId());

        // ---> کدهای جدید سئو <---
        product.setSeoTitle(request.getSeoTitle());
        product.setSeoDescription(request.getSeoDescription());
        String slugBase = (request.getSlug() == null || request.getSlug().trim().isEmpty())
                ? request.getName() : request.getSlug();
        product.setSlug(generateUniqueSlug(slugBase, product.getId()));
        // ---> پایان کدهای جدید سئو <---

        applyRichContent(product, request);

        return productRepo.save(product);
    }

    // متد مخصوص پنل ادمین (بدون صفحه‌بندی)
    public List<Product> getAllProductsForAdmin() {
        return productRepo.findAll();
    }

    // دریافت یک محصول با اسلاگ یا شناسه (برای صفحه محصول بدون نیاز به لود کل لیست)
    public Optional<Product> findBySlugOrId(String slugOrId) {
        Optional<Product> bySlug = productRepo.findBySlug(slugOrId);
        if (bySlug.isPresent()) return bySlug;
        return productRepo.findById(slugOrId);
    }

    /*
       ساخت اسلاگ تمیز و یکتا:
       - فاصله‌ها به خط تیره تبدیل می‌شوند
       - کاراکترهای خاص (/ ? # % ...) حذف می‌شوند تا URL نشکند
       - حروف/ارقام فارسی حفظ می‌شوند
       - در صورت تکراری بودن، یک شماره به انتها اضافه می‌شود (کالای اشتباه نمایش داده نشود)
    */
    private String generateUniqueSlug(String base, String excludeId) {
        String slug = slugify(base);
        if (slug.isEmpty()) slug = "product";

        String candidate = slug;
        int i = 2;
        while (true) {
            Optional<Product> existing = productRepo.findBySlug(candidate);
            if (existing.isEmpty() || existing.get().getId().equals(excludeId)) {
                return candidate;
            }
            candidate = slug + "-" + i++;
        }
    }

    private String slugify(String s) {
        return org.example.shop1.model.service.util.SlugUtil.slugify(s);
    }

    /*
       جدول مشخصات فنی، پرسش‌های متداول و محصولات مرتبط (با پاکسازی متن‌ها).
       ردیف‌های خالی حذف می‌شوند.
    */
    private void applyRichContent(Product product, ProductRequest request) {
        // دُمِ فارسیِ آدرسِ هیبرید (اختیاری؛ اگر خالی بماند هنگام رندر از نامِ محصول ساخته می‌شود)
        if (request.getPersianSlug() != null) {
            String pt = org.example.shop1.model.service.util.SlugUtil.sanitizeTail(request.getPersianSlug());
            product.setPersianSlug(pt.isEmpty() ? null : pt);
        }

        if (request.getTechSpecs() != null) {
            List<org.example.shop1.model.entity.TechSpecRow> rows = new java.util.ArrayList<>();
            for (org.example.shop1.model.entity.TechSpecRow r : request.getTechSpecs()) {
                if (r == null) continue;
                String k = org.example.shop1.config.SecurityUtils.clean(r.getKey());
                String v = org.example.shop1.config.SecurityUtils.clean(r.getValue());
                if (k == null || k.isBlank() || v == null || v.isBlank()) continue;
                rows.add(new org.example.shop1.model.entity.TechSpecRow(
                        org.example.shop1.config.SecurityUtils.clean(r.getGroup()), k.trim(), v.trim()));
            }
            product.setTechSpecs(rows);
        }

        if (request.getFaqs() != null) {
            List<org.example.shop1.model.entity.FaqItem> faqs = new java.util.ArrayList<>();
            for (org.example.shop1.model.entity.FaqItem f : request.getFaqs()) {
                if (f == null) continue;
                String q = org.example.shop1.config.SecurityUtils.clean(f.getQuestion());
                String a = org.example.shop1.config.SecurityUtils.clean(f.getAnswer());
                if (q == null || q.isBlank() || a == null || a.isBlank()) continue;
                faqs.add(new org.example.shop1.model.entity.FaqItem(q.trim(), a.trim()));
            }
            product.setFaqs(faqs);
        }

        if (request.getRelatedProductIds() != null) {
            List<String> ids = request.getRelatedProductIds().stream()
                    .filter(id -> id != null && !id.isBlank())
                    .filter(id -> !id.equals(product.getId())) // خودش نباشد
                    .distinct().limit(10).collect(Collectors.toList());
            product.setRelatedProductIds(ids);
        }
    }

    // متد اصلی برای فرانت‌اند (با قابلیت فیلتر، سورتینگ و صفحه‌بندی)
// در فایل ProductService.java متد را به این شکل اصلاح کنید:

    public Page<Product> getProductsForClient(
            String categoryId,
            String sectionId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sortBy,
            String sortDirection,
            int page,
            int size) {

        Sort sort = defineSort(sortBy, sortDirection);
        Pageable pageable = PageRequest.of(page, size, sort);

        // فیلتر جشنواره (لندینگ سکشن) — سمت سرور تا فرانت نیازی به لود همه محصولات نداشته باشد
        if (sectionId != null && !sectionId.trim().isEmpty()) {
            return productRepo.findBySectionIdsContaining(sectionId, pageable);
        }

        if (categoryId != null && !categoryId.trim().isEmpty()) {
            // ۱. پیدا کردن تمام دسته‌هایی که این دسته‌بندی والد یا جدّ آن‌هاست
            // از فیلد ancestors که در کدهای CategoryService شما پر می‌شود استفاده می‌کنیم
            List<Category> subCategories = categoryRepo.findAll().stream()
                    .filter(c -> c.getAncestors().contains(categoryId) || c.getId().equals(categoryId))
                    .collect(Collectors.toList());

            // ۲. استخراج تمام IDها
            List<String> allCategoryIds = subCategories.stream()
                    .map(Category::getId)
                    .collect(Collectors.toList());

            // ۳. فراخوانی متد جدید ریپازیتوری
            return productRepo.findByCategoryIdIn(allCategoryIds, pageable);
        }

        return productRepo.findAll(pageable);
    }

    // متد تعریف استاندارد سورتینگ
    private Sort defineSort(String sortBy, String sortDirection) {
        String sortField;
        Sort.Direction direction;

        switch (sortBy != null ? sortBy.toLowerCase() : "createdat") {
            case "salescount":
                sortField = "salesCount";
                direction = Sort.Direction.DESC; // پرفروش‌ترین: نزولی
                break;
            case "averagerating":
                sortField = "averageRating";
                direction = Sort.Direction.DESC; // محبوب‌ترین: نزولی
                break;
            case "price":
                // سورت بر اساس قیمت تخفیف‌خورده
                sortField = "discountedPrice";
                direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
                break;
            case "createdat":
            default:
                sortField = "createdAt";
                direction = Sort.Direction.DESC; // جدیدترین: نزولی
        }

        return Sort.by(direction, sortField);
    }

    public void deleteProduct(String id) {
        productRepo.deleteById(id);
    }
}