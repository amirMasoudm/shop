package org.example.shop1.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.shop1.config.SecurityUtils;
import org.example.shop1.model.dto.ProductRequest;
import org.example.shop1.model.dto.ProductSearchRequestDto;
import org.example.shop1.model.dto.ProductSearchResponseDto;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.service.ProductSearchService;
import org.example.shop1.model.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@CrossOrigin
public class ProductController {

    private final ProductService productService;
    private final ProductSearchService productSearchService;
    public ProductController(ProductService productService, ProductSearchService productSearchService) {
        this.productService = productService;
        this.productSearchService = productSearchService;
    }
    // در بالای فایل:


    // sitemap.xml به StoreWebController (روتِ /sitemap.xml) منتقل شد

    // اندپوینت جدید قدرتمند ما:
    @PostMapping("/search")
    public ResponseEntity<ProductSearchResponseDto> advancedSearch(@RequestBody ProductSearchRequestDto request) {
        ProductSearchResponseDto response = productSearchService.searchAndFilter(request);
        return ResponseEntity.ok(response);
    }

    // ۲. متد ایجاد محصول (پاکسازی امنیت XSS)
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody ProductRequest request) {
        request.setName(SecurityUtils.clean(request.getName()));
        request.setDescription(SecurityUtils.clean(request.getDescription()));
        request.setSeoTitle(SecurityUtils.clean(request.getSeoTitle()));
        request.setSeoDescription(SecurityUtils.clean(request.getSeoDescription()));

        Product createdProduct = productService.createProduct(request);
        return ResponseEntity.ok(createdProduct);
    }
    // متد مخصوص پنل ادمین (دریافت لیست کامل برای جدول ادمین)
    @GetMapping("/admin")
    public List<Product> getAllProductsForAdmin() {
        return productService.getAllProductsForAdmin();
    }

    // متد اصلی برای فرانت‌اند (با فیلترینگ، سورتینگ و صفحه‌بندی)
    @GetMapping
    public ResponseEntity<Page<Product>> getProductsForClient(
            @RequestParam(name = "categoryId", required = false) String categoryId,
            @RequestParam(name = "sectionId", required = false) String sectionId, // فیلتر جشنواره
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice, // فیلتر از سمت فرانت
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice, // فیلتر از سمت فرانت
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "sortDirection", required = false) String sortDirection,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Page<Product> products = productService.getProductsForClient(
                categoryId, sectionId, minPrice, maxPrice, sortBy, sortDirection, page, size);
        return ResponseEntity.ok(products);
    }

    // دریافت یک محصول با اسلاگ یا شناسه (برای صفحه محصول، بدون لود کل لیست)
    @GetMapping("/single/{slugOrId}")
    public ResponseEntity<Product> getSingleProduct(@PathVariable String slugOrId) {
        return productService.findBySlugOrId(slugOrId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new org.example.shop1.exeption.ApiException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "محصول یافت نشد"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable String id, @RequestBody ProductRequest request) {
        Product updatedProduct = productService.updateProduct(id, request);
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}