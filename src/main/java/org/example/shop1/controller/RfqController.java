package org.example.shop1.controller;

import org.example.shop1.model.dto.RfqRequestDto;
import org.example.shop1.model.entity.Order;
import org.example.shop1.model.entity.Rfq;
import org.example.shop1.model.service.RfqService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rfq")
@CrossOrigin
public class RfqController {

    private final RfqService rfqService;

    public RfqController(RfqService rfqService) {
        this.rfqService = rfqService;
    }

    // ======== کاربر (نیازمند ورود) ========

    @PostMapping
    public ResponseEntity<Rfq> create(@RequestBody RfqRequestDto.CreateRequest request) {
        return ResponseEntity.ok(rfqService.createRfq(request));
    }

    @GetMapping("/my")
    public List<Rfq> myRfqs() {
        return rfqService.getMyRfqs();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Rfq> getOne(@PathVariable String id) {
        return ResponseEntity.ok(rfqService.getRfqById(id));
    }

    @PostMapping("/{id}/offer")
    public ResponseEntity<Rfq> offer(@PathVariable String id, @RequestBody RfqRequestDto.OfferRequest req) {
        return ResponseEntity.ok(rfqService.userOffer(id, req));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<Order> accept(@PathVariable String id) {
        return ResponseEntity.ok(rfqService.acceptRfq(id));
    }

    // ======== ادمین (مسیر admin در SecurityConfig فقط ROLE_ADMIN) ========

    @GetMapping("/admin/all")
    public List<Rfq> adminAll() {
        return rfqService.getAllForAdmin();
    }

    @PostMapping("/admin/{id}/quote")
    public ResponseEntity<Rfq> adminQuote(@PathVariable String id, @RequestBody RfqRequestDto.OfferRequest req) {
        return ResponseEntity.ok(rfqService.adminQuote(id, req));
    }

    @PostMapping("/admin/{id}/reject")
    public ResponseEntity<Rfq> adminReject(@PathVariable String id) {
        return ResponseEntity.ok(rfqService.adminReject(id));
    }
}
