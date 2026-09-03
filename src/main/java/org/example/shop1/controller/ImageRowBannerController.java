package org.example.shop1.controller;

import org.example.shop1.model.entity.ImageRowBanner;
import org.example.shop1.model.service.ImageRowBannerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** پنجره‌هایِ ردیفیِ بنرِ تصویری — الگویِ دقیقاً مشابهِ Banner/EducationArchive. */
@RestController
@RequestMapping("/api/v1/image-row-banners")
@CrossOrigin
public class ImageRowBannerController {

    private final ImageRowBannerService service;

    public ImageRowBannerController(ImageRowBannerService service) {
        this.service = service;
    }

    // --- عمومی --- همه‌ی پنجره‌های فعال (هر جایگاهی)؛ کلاینت خودش بر اساسِ
    // placement/afterSectionId فیلتر می‌کند (دقیقاً مثلِ /api/v1/banners/active).
    @GetMapping("/active")
    public List<ImageRowBanner> active() {
        return service.getAllActive();
    }

    // --- ادمین ---
    @GetMapping("/admin")
    public List<ImageRowBanner> admin() {
        return service.getAllForAdmin();
    }

    @PostMapping("/admin")
    public ImageRowBanner save(@RequestBody ImageRowBanner banner) {
        return service.save(banner);
    }

    @DeleteMapping("/admin/{id}")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
