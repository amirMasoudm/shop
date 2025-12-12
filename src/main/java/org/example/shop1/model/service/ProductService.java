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
        product.setSpecifications(request.getSpecifications());

        // ۳. تنظیم فیلدهای سورتینگ اولیه
        product.setCreatedAt(Instant.now());
        product.setSalesCount(0L);
        product.setReviewCount(0L);
        product.setAverageRating(0.0);

        // ۴. اعمال تخفیف و ذخیره
        applyDiscount(product, request.getDiscountPercent());

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

        if (!product.getCategoryId().equals(request.getCategoryId())) {
            Category category = categoryRepo.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategoryId(category.getId());
        }

        product.setImages(request.getImages());
        product.setSpecifications(request.getSpecifications());

        // اعمال مجدد تخفیف با درصد جدید
        applyDiscount(product, request.getDiscountPercent());

        return productRepo.save(product);
    }

    // متد مخصوص پنل ادمین (بدون صفحه‌بندی)
    public List<Product> getAllProductsForAdmin() {
        return productRepo.findAll();
    }

    // متد اصلی برای فرانت‌اند (با قابلیت فیلتر، سورتینگ و صفحه‌بندی)
    public Page<Product> getProductsForClient(
            String categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sortBy,
            String sortDirection,
            int page,
            int size) {

        Sort sort = defineSort(sortBy, sortDirection);
        Pageable pageable = PageRequest.of(page, size, sort);

        // **نکته:** منطق فیلتر قیمت (minPrice/maxPrice) و فیلتر مشخصات
        // در حال حاضر نیاز به پیاده‌سازی کوئری‌های پیشرفته در ریپازیتوری دارد.

        // مثال پیاده‌سازی فیلتر CategoryId:
        if (categoryId != null && !categoryId.trim().isEmpty()) {
            // در حالت واقعی باید فیلتر قیمت را نیز اینجا اعمال کنید
            return productRepo.findByCategoryId(categoryId, pageable);
        }

        // بدون فیلتر خاص (فقط سورتینگ و صفحه‌بندی)
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