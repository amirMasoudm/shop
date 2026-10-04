package org.example.shop1.controller;

import org.example.shop1.model.entity.ProductRedirect;
import org.example.shop1.model.service.ProductRedirectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * مدیریتِ نگاشتِ «اسلاگِ قدیمی → محصولِ زنده». فقط ادمین (قفل در {@code SecurityConfig}).
 * <p>
 * گردشِ کارِ امنِ ادغام: <b>اول ریدایرکت را ثبت کن، بعد رکوردِ بازنده را حذف کن</b> —
 * برعکسش پنجره‌ای می‌سازد که آدرس ۴۰۴ می‌دهد.
 */
@RestController
@RequestMapping("/api/v1/product-redirects")
public class ProductRedirectController {

    private final ProductRedirectService service;

    public ProductRedirectController(ProductRedirectService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductRedirect> list() {
        return service.findAll();
    }

    /** بدنه: {@code {"fromSlug": "...", "toResolver": "...", "note": "..."}} */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, String> body) {
        ProductRedirect saved = service.create(
                body.get("fromSlug"), body.get("toResolver"), body.get("note"));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("redirect", saved);
        // مقصدِ حل‌نشدنی خطا نیست (زنجیره و ترتیبِ امنِ ادغام هر دو مجازند)، ولی
        // بی‌صدا هم نباید بماند — تایپوی مقصد باید همین‌جا دیده شود.
        boolean resolves = service.targetResolves(saved.getToResolver());
        response.put("targetResolves", resolves);
        if (!resolves) {
            response.put("warning", "مقصد فعلاً به هیچ محصولِ زنده‌ای نمی‌رسد؛ تا وقتی حل نشود این اسلاگ ۴۰۴ می‌دهد.");
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
