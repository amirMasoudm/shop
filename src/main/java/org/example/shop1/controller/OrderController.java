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
    // متد جدید: شروع فرآیند پرداخت و بازگرداندن لینک درگاه تستی
    @PostMapping("/{orderId}/pay")
    public ResponseEntity<?> initiatePayment(@PathVariable String orderId, HttpServletRequest request) {
        try {
            // آدرس درگاه را بر اساس آدرس فعلی سرور میسازیم
            String baseUrl = request.getContextPath();
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("paymentUrl", baseUrl + "/api/orders/mock-gateway/" + orderId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // متد جدید: نمایش صفحه وب درگاه پرداخت تستی بانک
// متد اصلاح‌شده: نمایش صفحه وب درگاه پرداخت تستی بانک با مکانیزم ایمن جایگزینی رشته
    @GetMapping(value = "/mock-gateway/{orderId}", produces = org.springframework.http.MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String mockGatewayPage(@PathVariable String orderId) {
        String html = """
            <!DOCTYPE html>
            <html lang="fa" dir="rtl">
            <head>
                <meta charset="UTF-8">
                <title>درگاه پرداخت تستی | داده نما</title>
                <style>
                    body { font-family: Tahoma, Arial, sans-serif; background: #f3f4f6; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; }
                    .card { background: white; padding: 40px; border-radius: 16px; box-shadow: 0 10px 25px rgba(0,0,0,0.05); text-align: center; max-width: 400px; width: 100%; border: 1px solid #e5e7eb; }
                    .logo { font-size: 48px; margin-bottom: 20px; }
                    h2 { margin: 0 0 10px 0; color: #1f2937; }
                    p { color: #6b7280; font-size: 13px; margin-bottom: 30px; line-height: 1.5; }
                    .btn { display: block; width: 100%; padding: 14px; margin: 10px 0; border: none; border-radius: 10px; font-weight: bold; cursor: pointer; transition: 0.2s; font-size: 14px; }
                    .btn-success { background: #10b981; color: white; }
                    .btn-success:hover { background: #059669; }
                    .btn-fail { background: #ef4444; color: white; }
                    .btn-fail:hover { background: #dc2626; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="logo">💳</div>
                    <h2>درگاه پرداخت شبیه‌سازی شده داده نما</h2>
                    <p>شما در حال پرداخت تستی سفارش شماره <br><strong>{{orderId}}</strong> هستید.</p>
                    
                    <form action="/api/orders/mock-gateway-callback/{{orderId}}" method="POST">
                        <input type="hidden" name="status" value="SUCCESS">
                        <button type="submit" class="btn btn-success">شبیه‌سازی پرداخت موفقیت‌آمیز ✅</button>
                    </form>
                    
                    <form action="/api/orders/mock-gateway-callback/{{orderId}}" method="POST">
                        <input type="hidden" name="status" value="FAILED">
                        <button type="submit" class="btn btn-fail">انصراف از پرداخت (خطا) ❌</button>
                    </form>
                </div>
            </body>
            </html>
            """;

        // جایگزینی امن شناسه سفارش به جای استفاده از قالب‌بندی درصدی
        return html.replace("{{orderId}}", orderId);
    }


    // متد جدید: هندل کردن پاسخ درگاه و بازگرداندن کاربر به پنل کاربری با اعمال وضعیت پرداخت
    @PostMapping("/mock-gateway-callback/{orderId}")
    public ResponseEntity<Void> mockCallback(@PathVariable String orderId, @RequestParam String status) {
        boolean paid = false;
        try {
            if ("SUCCESS".equals(status)) {
                // ۱. تغییر وضعیت سفارش به پرداخت شده — فقط برای صاحب همان سفارش
                orderService.payOrderByCurrentUser(orderId);
                paid = true;
                log.info("سفارش شماره {} با موفقیت پرداخت شد و به مرحله بسته‌بندی رفت.", orderId);
            } else {
                log.warn("پرداخت سفارش شماره {} ناموفق بود.", orderId);
            }
        } catch (Exception e) {
            log.error("خطا در ثبت تراکنش: {}", e.getMessage());
        }

        // پارامتر paid برای رویداد purchase در آنالیتیکس پنل کاربری
        String location = paid ? ("/profile?paid=1&order=" + orderId) : "/profile";
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, location)
                .build();
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