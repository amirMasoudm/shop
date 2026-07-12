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


    // ۱. اضافه کردن ورودی HttpServletRequest به متد سitemap
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String getSitemap(HttpServletRequest request) {
        // تشخیص خودکار دامنه سرور (بدون هاردکد کردن)
        String baseUrl = request.getScheme() + "://" + request.getServerName() +
                (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort());

        List<Product> products = productService.getAllProductsForAdmin();
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");

        xml.append("<url><loc>").append(baseUrl).append("/</loc><priority>1.0</priority></url>");

        for (Product p : products) {
            String slug = (p.getSlug() != null) ? p.getSlug() : p.getId();
            xml.append("<url>");
            xml.append("<loc>").append(baseUrl).append("/product/").append(slug).append("</loc>");
            xml.append("<lastmod>").append(p.getUpdatedAt().toString().substring(0,10)).append("</lastmod>");
            xml.append("<priority>0.8</priority>");
            xml.append("</url>");
        }
        xml.append("</urlset>");
        return xml.toString();
    }



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
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice, // فیلتر از سمت فرانت
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice, // فیلتر از سمت فرانت
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "sortDirection", required = false) String sortDirection,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
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