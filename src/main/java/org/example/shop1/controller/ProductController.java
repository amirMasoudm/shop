package org.example.shop1.controller;

import org.example.shop1.model.dto.ProductRequest;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@CrossOrigin
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // متد ایجاد محصول
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody ProductRequest request) {
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
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) BigDecimal minPrice, // فیلتر از سمت فرانت
            @RequestParam(required = false) BigDecimal maxPrice, // فیلتر از سمت فرانت
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Product> products = productService.getProductsForClient(
                categoryId, minPrice, maxPrice, sortBy, sortDirection, page, size);

        return ResponseEntity.ok(products);
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