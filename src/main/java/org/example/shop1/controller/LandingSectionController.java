package org.example.shop1.controller;

import org.example.shop1.model.entity.LandingSection;
import org.example.shop1.model.service.LandingSectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/landing-sections")
@CrossOrigin
public class LandingSectionController {

    private final LandingSectionService sectionService;

    public LandingSectionController(LandingSectionService sectionService) {
        this.sectionService = sectionService;
    }

    // --- API های مربوط به فرانت‌اند (CL.html) ---

    // دیافت لیست سکشن‌های فعال با ترتیب درست
    @GetMapping("/active")
    public ResponseEntity<List<LandingSection>> getActiveSections() {
        return ResponseEntity.ok(sectionService.getActiveSectionsForLanding());
    }

    // --- API های مربوط به ادمین (Admin.html) ---

    @GetMapping("/admin")
    public ResponseEntity<List<LandingSection>> getAllSectionsForAdmin() {
        return ResponseEntity.ok(sectionService.getAllForAdmin());
    }

    @PostMapping
    public ResponseEntity<LandingSection> createOrUpdateSection(@RequestBody LandingSection section) {
        return ResponseEntity.ok(sectionService.saveSection(section));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSection(@PathVariable String id) {
        sectionService.deleteSection(id);
        return ResponseEntity.noContent().build();
    }
}