package org.example.shop1.controller;

import org.example.shop1.model.entity.EducationArchiveItem;
import org.example.shop1.model.service.EducationArchiveService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** الگویِ دقیقاً مشابهِ BannerController — CRUDِ ادمین + یک اندپوینتِ عمومیِ فقط-فعال‌ها. */
@RestController
@RequestMapping("/api/v1/education-archive")
@CrossOrigin
public class EducationArchiveController {

    private final EducationArchiveService service;

    public EducationArchiveController(EducationArchiveService service) {
        this.service = service;
    }

    // --- عمومی (home.html و صفحه‌ی /learn) ---
    @GetMapping("/active")
    public ResponseEntity<List<EducationArchiveItem>> getActive() {
        return ResponseEntity.ok(service.getActive());
    }

    // --- ادمین (Admin.html) ---
    @GetMapping("/admin")
    public ResponseEntity<List<EducationArchiveItem>> getAllForAdmin() {
        return ResponseEntity.ok(service.getAllForAdmin());
    }

    @PostMapping
    public ResponseEntity<EducationArchiveItem> save(@RequestBody EducationArchiveItem item) {
        return ResponseEntity.ok(service.save(item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
