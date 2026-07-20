package org.example.shop1.controller;

import org.example.shop1.config.SecurityUtils;
import org.example.shop1.model.dto.CategoryOrderDto;
import org.example.shop1.model.dto.CategoryRequestDto;
import org.example.shop1.model.dto.CategoryResponseDto;
import org.example.shop1.model.entity.Category;
import org.example.shop1.model.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }
    // متد getTree را تغییر دهید تا بر اساس نوع فیلتر کند
    @GetMapping("/tree")
    public List<CategoryResponseDto> getTree(@RequestParam(required = false) String type) {
        return service.getTree(type); // سرویس باید آپدیت شود
    }
// در فایل CategoryController.java

    @PostMapping
    public Category create(@RequestBody CategoryRequestDto dto) {
        // ۱. پاکسازی فیلدهای متنی ساده
        dto.setName(SecurityUtils.clean(dto.getName()));
        dto.setParentId(SecurityUtils.clean(dto.getParentId()));
        dto.setType(SecurityUtils.clean(dto.getType()));
        dto.setSlug(SecurityUtils.clean(dto.getSlug()));
        dto.setSeoTitle(SecurityUtils.clean(dto.getSeoTitle()));
        dto.setSeoDescription(SecurityUtils.clean(dto.getSeoDescription()));

        // ۲. پاکسازی لیست ویژگی‌های فیلتر (Filter Keys)
        if (dto.getFilterKeys() != null) {
            // تمام رشته‌های داخل لیست را یکی یکی تمیز می‌کند
            dto.getFilterKeys().replaceAll(SecurityUtils::clean);
        }

        return service.create(dto);
    }

    @PutMapping("/{id}")
    public Category update(@PathVariable String id, @RequestBody CategoryRequestDto dto) {
        // ۱. پاکسازی فیلدهای متنی
        dto.setName(SecurityUtils.clean(dto.getName()));
        dto.setParentId(SecurityUtils.clean(dto.getParentId()));
        dto.setNewParentId(SecurityUtils.clean(dto.getNewParentId()));
        dto.setType(SecurityUtils.clean(dto.getType()));
        dto.setSlug(SecurityUtils.clean(dto.getSlug()));
        dto.setSeoTitle(SecurityUtils.clean(dto.getSeoTitle()));
        dto.setSeoDescription(SecurityUtils.clean(dto.getSeoDescription()));

        // ۲. پاکسازی لیست ویژگی‌ها
        if (dto.getFilterKeys() != null) {
            dto.getFilterKeys().replaceAll(SecurityUtils::clean);
        }

        return service.update(id, dto);
    }

    // شمارش محصولات یک دسته و زیرمجموعه‌هایش (برای تصمیم‌گیری هنگام حذف)
    @GetMapping("/{id}/product-count")
    public ResponseEntity<Long> productCount(@PathVariable String id) {
        return ResponseEntity.ok(service.countProductsInSubtree(id));
    }

    // حذف با تعیین تکلیف محصولات: mode = BLOCK | REASSIGN | CASCADE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @RequestParam(defaultValue = "BLOCK") String mode,
            @RequestParam(required = false) String targetCategoryId) {
        service.delete(id, SecurityUtils.clean(mode), SecurityUtils.clean(targetCategoryId));
        return ResponseEntity.ok().build();
    }

//    @GetMapping("/tree")
//    public List<CategoryResponseDto> getTree() {
//        return service.getTree();
//    }

    @PutMapping("/{id}/move")     public Category move(@PathVariable String id, @RequestBody CategoryRequestDto dto) {
        return service.moveCategory(id, dto.getNewParentId());
    }

    // مرتب‌سازی/جابجایی کل درخت با یک درخواست (درگ‌دراپ)
    @PutMapping("/reorder")
    public ResponseEntity<Void> reorder(@RequestBody List<CategoryOrderDto> items) {
        service.reorder(items);
        return ResponseEntity.ok().build();
    }
}
