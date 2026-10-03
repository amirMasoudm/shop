package org.example.shop1.controller;

import org.example.shop1.model.entity.Order;
import org.example.shop1.model.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales/online")
//@CrossOrigin // برای اتصال به فرانت
public class OnlineSalesController {

    private final OrderService orderService;

    public OnlineSalesController(OrderService orderService) {
        this.orderService = orderService;
    }
    // API جدید: گرفتن سفارشات من
    @GetMapping("/my")
    public List<Order> getMyOrders() {
        return orderService.getMyOrders();
    }
    // API آمار
//    @GetMapping("/stats")
//    public ResponseEntity<Map<String, Object>> getOnlineStats() {
//        Map<String, Object> stats = orderService.calculateOnlineStats();
//        return ResponseEntity.ok(stats);
//    }
//
//    // در کنترلر مربوطه (احتمالاً AdminController یا مشابه آن)
//    @GetMapping("/orders")
//    public ResponseEntity<List<Order>> getRecentOrders(@RequestParam(name = "limit", defaultValue = "10") int limit) {
//        List<Order> orders = orderService.getRecentOnlineOrders(limit);
//        return ResponseEntity.ok(orders);
//    }
    // API ایجاد سفارش جدید (از فرانت یا تست)
//    @PostMapping("/orders")
//    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
//        order.setType("ONLINE"); // ثابت برای آنلاین
//        Order created = orderService.createOrder(order);
//        return ResponseEntity.ok(created);
//    }

    // API بروزرسانی وضعیت
//    @PutMapping("/orders/{id}/status")
//    public ResponseEntity<Order> updateStatus(@PathVariable String id, @RequestBody String newStatus) {
//        Order updated = orderService.updateOrderStatus(id, newStatus);
//        return ResponseEntity.ok(updated);
//    }
}