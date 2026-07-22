package org.example.shop1.controller;

import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.Article;
import org.example.shop1.model.service.ArticleService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/articles")
public class ArticleController {

    private final ArticleService service;

    public ArticleController(ArticleService service) {
        this.service = service;
    }

    // ================= عمومی (فقط خواندن) =================

    // لیست مقالات منتشرشده (برای صفحه بلاگ)
    @GetMapping
    public ResponseEntity<Page<Article>> listPublished(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size) {
        return ResponseEntity.ok(service.getPublished(page, size));
    }

    // یک مقاله‌ی منتشرشده با اسلاگ یا شناسه
    @GetMapping("/single/{slugOrId}")
    public ResponseEntity<Article> getSingle(@PathVariable String slugOrId) {
        return service.getPublishedBySlugOrId(slugOrId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "مقاله یافت نشد"));
    }

    // ================= ادمین (زیر /admin — محافظت‌شده در SecurityConfig) =================

    @GetMapping("/admin/all")
    public List<Article> allForAdmin() {
        return service.getAllForAdmin();
    }

    @PostMapping("/admin")
    public ResponseEntity<Article> save(@RequestBody Article article) {
        return ResponseEntity.ok(service.save(article));
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
