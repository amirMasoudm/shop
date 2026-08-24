package org.example.shop1.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.shop1.exeption.InsufficientStockException;
import org.example.shop1.model.dto.OrderRequestDto;
import org.example.shop1.model.dto.ShippingOption;
import org.example.shop1.model.entity.Address;
import org.example.shop1.model.entity.Order;
import org.example.shop1.model.enums.OrderStatus;
import org.example.shop1.model.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/create-draft")
    public ResponseEntity<?> createDraft(@RequestBody OrderRequestDto request) {
        try {
            return ResponseEntity.ok(orderService.createDraftOrder(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // متد جدید: بروزرسانی تعداد اقلام از فرانت‌اند در مرحله 1 فاکتور
    @PutMapping("/{id}/items")
    public ResponseEntity<?> updateOrderItems(
            @PathVariable String id,
            @RequestBody List<OrderRequestDto.CartItemDto> items) {
        try {
            return ResponseEntity.ok(orderService.updateOrderItems(id, items));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // متد جدید: حذف و لغو سفارش پرداخت‌نشده توسط کاربر
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable String id) {
        try {
            orderService.deleteOrder(id); // صدا زدن متد حذف در سرویس
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/{id}/shipping-quotes")
    public ResponseEntity<List<ShippingOption>> getShippingQuotes(
            @PathVariable String id,
            @RequestBody Address address) {
        return ResponseEntity.ok(orderService.getShippingQuotes(id, address));
    }

    @PostMapping("/{orderId}/finalize")
    public ResponseEntity<?> finalizeOrder(
            @PathVariable String orderId,
            @RequestBody OrderRequestDto request) {
        try {
            return ResponseEntity.ok(orderService.finalizeOrder(orderId, request));
        } catch (InsufficientStockException e) {
            // بازگرداندن وضعیت 409 همراه با فاکتور جدید اصلاح شده به فرانت‌سرا
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(e.getUpdatedOrder());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/my/orders")
    public List<Order> getMyOrders() {
        return orderService.getMyOrders();
    }

    @GetMapping("/admin/all")
    public List<Order> getAllOrders() {
        return orderService.getAllOrdersForAdmin();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable String id) {
        try {
            return ResponseEntity.ok(orderService.getOrderById(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // شروعِ پرداختِ واقعی: bpPayRequest می‌زند و آدرسِ صفحه‌ی auto-submitِ داخلی را برمی‌گرداند
    @PostMapping("/{orderId}/pay")
    public ResponseEntity<?> initiatePayment(@PathVariable String orderId, HttpServletRequest request) {
        try {
            Map<String, String> response = orderService.startMellatPayment(orderId, buildBaseUrl(request));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // صفحه‌ی auto-submit که کاربر را با POSTِ RefId به درگاهِ واقعیِ ملت می‌فرستد
    @GetMapping(value = "/mellat-redirect/{orderId}", produces = org.springframework.http.MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> mellatRedirectPage(@PathVariable String orderId) {
        try {
            return ResponseEntity.ok(orderService.buildMellatRedirectHtml(orderId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("<p style=\"font-family:Tahoma\">" + e.getMessage() + "</p>");
        }
    }

    // callbackِ واقعیِ بانک — از سرورِ ملت/مرورگرِ کاربر می‌آید (بدونِ سشن)؛ در SecurityConfig
    // permitAll و معافِ CSRF است (نمونه: Torob). امنیتِ واقعی با bpVerifyRequest تضمین می‌شود،
    // نه با اعتماد به همین POST — رجوع به OrderService.handleMellatCallback.
    @PostMapping("/mellat-callback")
    public ResponseEntity<Void> mellatCallback(@RequestParam Map<String, String> params) {
        String location;
        try {
            location = orderService.handleMellatCallback(params);
        } catch (Exception e) {
            log.error("خطایِ پیش‌بینی‌نشده در پردازشِ callbackِ ملت: {}", e.toString());
            location = "/profile?paymentError=1";
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, location)
                .build();
    }

    /** hostname/scheme از خودِ ریکوئست — برایِ callBackUrlِ ارسالی به بانک باید مطلق باشد. */
    private String buildBaseUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() +
                (request.getServerPort() == 80 || request.getServerPort() == 443
                        ? "" : ":" + request.getServerPort());
    }

    // متد جدید: تغییر وضعیت سفارش توسط ادمین به مراحل بعدی
    @PutMapping("/admin/{orderId}/status")
    public ResponseEntity<?> updateStatusByAdmin(
            @PathVariable String orderId,
            @RequestParam OrderStatus status) {
        try {
            return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}