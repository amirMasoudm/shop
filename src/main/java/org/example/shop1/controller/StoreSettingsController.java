package org.example.shop1.controller;

import org.example.shop1.model.entity.StoreSettings;
import org.example.shop1.model.service.StoreSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
@CrossOrigin
public class StoreSettingsController {

    private final StoreSettingsService settingsService;

    public StoreSettingsController(StoreSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/store-location")
    public ResponseEntity<StoreSettings> getStoreLocation() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PostMapping("/store-location")
    public ResponseEntity<StoreSettings> saveStoreLocation(@RequestBody StoreSettings settings) {
        return ResponseEntity.ok(settingsService.updateSettings(settings));
    }
}