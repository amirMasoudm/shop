package org.example.shop1.controller;

import org.example.shop1.model.dto.BannerDisplayDto;
import org.example.shop1.model.entity.Banner;
import org.example.shop1.model.service.BannerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** الگویِ دقیقاً مشابهِ LandingSectionController — CRUDِ ادمین + یک اندپوینتِ عمومیِ فقط-فعال‌ها. */
@RestController
@RequestMapping("/api/v1/banners")
@CrossOrigin
public class BannerController {

    private final BannerService bannerService;

    public BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    // --- عمومی (CL.html) ---
    @GetMapping("/active")
    public ResponseEntity<List<BannerDisplayDto>> getActiveBanners() {
        return ResponseEntity.ok(bannerService.getActiveBannersResolved());
    }

    // --- ادمین (Admin.html) ---
    @GetMapping("/admin")
    public ResponseEntity<List<Banner>> getAllForAdmin() {
        return ResponseEntity.ok(bannerService.getAllForAdmin());
    }

    @PostMapping
    public ResponseEntity<Banner> save(@RequestBody Banner banner) {
        return ResponseEntity.ok(bannerService.save(banner));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        bannerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
