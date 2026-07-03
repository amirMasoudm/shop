package org.example.shop1.model.service;

import org.example.shop1.exeption.InsufficientStockException;
import org.example.shop1.model.dto.OrderRequestDto;
import org.example.shop1.model.dto.ShippingOption;
import org.example.shop1.model.entity.*;
import org.example.shop1.model.enums.OrderStatus;
import org.example.shop1.model.enums.Role;
import org.example.shop1.model.reposritory.OrderRepository;
import org.example.shop1.model.reposritory.ProductRepository;
import org.example.shop1.model.reposritory.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;
    private final ShippingService shippingService;

    public OrderService(OrderRepository orderRepo, ProductRepository productRepo, UserRepository userRepo, ShippingService shippingService) {
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
        this.userRepo = userRepo;
        this.shippingService = shippingService;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userRepo.findByUsername(auth.getName()).orElseThrow(() -> new RuntimeException("User not found"));
        }
        return userRepo.findByUsername("09120000000").orElseGet(() -> {
            User testUser = new User("09120000000", Role.USER);
            testUser.setFirstName("کاربر"); testUser.setLastName("تستی");
            return userRepo.save(testUser);
        });
    }

    @Transactional
    public Order createDraftOrder(OrderRequestDto request) {
        User user = getCurrentUser();
        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(Instant.now());
        order.setStatus(OrderStatus.PENDING_PAYMENT); // اصلاح هماهنگ با کلاس الگوی تعریف شده
        order.setType("ONLINE");
        order.setOrderCode("ORD-" + (System.currentTimeMillis() % 1000000));

        return updateOrderItemsLogic(order, request.getItems());
    }

    // متد لغو و حذف سفارش برای اضافه کردن به کلاس OrderService
    public void deleteOrder(String id) {
        // ۱. پیدا کردن سفارش
        Order order = orderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("سفارش مورد نظر یافت نشد: " + id));

        // ۲. امنیت: بررسی اینکه سفارش فقط در وضعیت پرداخت‌نشده باشد
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new RuntimeException("امکان لغو یا حذف سفارش پرداخت شده وجود ندارد.");
        }

        // ۳. حذف فیزیکی از دیتابیس
        orderRepo.delete(order);
    }

    // متد جدید: بروزرسانی سبد خرید قبل از پرداخت
    @Transactional
    public Order updateOrderItems(String orderId, List<OrderRequestDto.CartItemDto> items) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("سفارش پیدا نشد"));
        if (order.isFinalized()) throw new RuntimeException("امکان ویرایش سفارش نهایی شده وجود ندارد");
        return updateOrderItemsLogic(order, items);
    }

    private Order updateOrderItemsLogic(Order order, List<OrderRequestDto.CartItemDto> dtos) {
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal itemsTotal = BigDecimal.ZERO;

        for (OrderRequestDto.CartItemDto itemDto : dtos) {
            Product product = productRepo.findById(itemDto.getProductId()).orElseThrow(() -> new RuntimeException("محصول پیدا نشد"));

            BigDecimal finalPrice = product.getOnlinePrice() != null ? product.getOnlinePrice() : product.getPrice();
            if (product.getDiscountPercent() != null && product.getDiscountPercent() > 0) {
                BigDecimal discount = finalPrice.multiply(BigDecimal.valueOf(product.getDiscountPercent())).divide(BigDecimal.valueOf(100));
                finalPrice = finalPrice.subtract(discount);
            }

            String img = (product.getImages() != null && !product.getImages().isEmpty()) ? product.getImages().get(0) : null;
            orderItems.add(new OrderItem(product.getId(), product.getName(), img, finalPrice, itemDto.getQuantity()));
            itemsTotal = itemsTotal.add(finalPrice.multiply(BigDecimal.valueOf(itemDto.getQuantity())));
        }

        order.setItems(orderItems);
        order.setItemsTotal(itemsTotal);
        // تا قبل از انتخاب روش ارسال، هزینه ارسال صفر است
        order.setTotalAmount(itemsTotal.add(order.getShippingCost() != null ? order.getShippingCost() : BigDecimal.ZERO));
        return orderRepo.save(order);
    }

    public List<ShippingOption> getShippingQuotes(String orderId, Address destination) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("سفارش یافت نشد"));
        double totalWeight = 0;
        for (OrderItem item : order.getItems()) {
            Product product = productRepo.findById(item.getProductId()).orElse(null);
            if (product != null && product.getWeight() != null) {
                totalWeight += (product.getWeight() * item.getQuantity());
            }
        }
        return shippingService.calculateOptions(destination, totalWeight);
    }

    @Transactional
    public Order finalizeOrder(String orderId, OrderRequestDto request) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("سفارش پیدا نشد"));
        if (order.isFinalized()) throw new RuntimeException("این سفارش قبلاً نهایی شده");

        // اگر حین فینالایز اقلامی ارسال شد (محکم‌کاری)، آپدیت کن
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            updateOrderItemsLogic(order, request.getItems());
        }

        // =========================================================================
        // سیستم اعتبارسنجی زنده انبار پیش از ثبت نهایی (بدون کسر موجودی فیزیکی)
        // =========================================================================
        boolean stockModified = false;
        List<OrderItem> items = new ArrayList<>(order.getItems());
        Iterator<OrderItem> iterator = items.iterator();

        while (iterator.hasNext()) {
            OrderItem item = iterator.next();
            Product product = productRepo.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("محصول پیدا نشد: " + item.getProductName()));

            // اگر موجودی انبار کمتر از درخواست کاربر باشد
            if (product.getStock() < item.getQuantity()) {
                stockModified = true;
                if (product.getStock() <= 0) {
                    // حذف کامل کالا از سبد در صورت اتمام موجودی
                    iterator.remove();
                } else {
                    // کاهش تعداد درخواستی به میزان باقیمانده انبار
                    item.setQuantity(product.getStock());
                }
            }
        }

        // اگر تغییری در فاکتور رخ داده باشد، فاکتور جدید ذخیره شده و استثنای ۴۰۹ پرتاب می‌شود
        if (stockModified) {
            order.setItems(items);
            // محاسبه مجدد هزینه‌ها با اقلام ویرایش شده جدید
            BigDecimal newItemsTotal = BigDecimal.ZERO;
            for (OrderItem item : items) {
                newItemsTotal = newItemsTotal.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
            order.setItemsTotal(newItemsTotal);
            BigDecimal shippingCost = request.getShippingCost() != null ? request.getShippingCost() : BigDecimal.ZERO;
            order.setShippingCost(shippingCost);
            order.setTotalAmount(newItemsTotal.add(shippingCost));
            orderRepo.save(order);

            // پرتاب خطا جهت باخبر کردن کلاینت و نمایش پاپ‌آپ تایید جدید
            throw new InsufficientStockException(order);
        }
        // =========================================================================

        order.setShippingAddress(request.getAddress());
        BigDecimal shippingCost = request.getShippingCost() != null ? request.getShippingCost() : BigDecimal.ZERO;
        order.setShippingCost(shippingCost);
        order.setTotalAmount(order.getItemsTotal().add(shippingCost));

        order.setFinalized(true);
        return orderRepo.save(order);
    }

    public List<Order> getMyOrders() { return orderRepo.findByUserOrderByOrderDateDesc(getCurrentUser()); }
    public List<Order> getAllOrdersForAdmin() { return orderRepo.findAll(); }
    public Order getOrderById(String id) { return orderRepo.findById(id).orElseThrow(() -> new RuntimeException("سفارش پیدا نشد")); }

    // متد جدید جهت تغییر وضعیت سفارش در دیتابیس به صورت کاملا ایمن
    @Transactional
    public Order updateOrderStatus(String orderId, OrderStatus status) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("سفارش یافت نشد: " + orderId));

        // =========================================================================
        // کسر موجودی فیزیکی انبار کالاها دقیقا در لحظه تایید پرداخت (مرحله PAID_PREPARING)
        // =========================================================================
        if (status == OrderStatus.PAID_PREPARING && order.getStatus() != OrderStatus.PAID_PREPARING) {
            for (OrderItem item : order.getItems()) {
                Product product = productRepo.findById(item.getProductId())
                        .orElseThrow(() -> new RuntimeException("محصول پیدا نشد: " + item.getProductName()));

                if (product.getStock() < item.getQuantity()) {
                    // سناریو نادر: اگر در فاصله تایید فاکتور تا فشردن پرداخت موجودی تمام شده باشد
                    throw new RuntimeException("متأسفانه در زمان پرداخت، موجودی کالا به اتمام رسید: " + product.getName());
                }

                // کسر نهایی از انبار
                product.setStock(product.getStock() - item.getQuantity());
                productRepo.save(product);
            }
        }
        // =========================================================================

        order.setStatus(status);
        return orderRepo.save(order);
    }
}