package org.example.shop1.model.service;

import org.example.shop1.exeption.InsufficientStockException;
import org.example.shop1.model.dto.OrderRequestDto;
import org.example.shop1.model.dto.ShippingOption;
import org.example.shop1.model.entity.*;
import org.example.shop1.model.enums.OrderItemType;
import org.example.shop1.model.enums.OrderStatus;
import org.example.shop1.model.enums.Role;
import org.example.shop1.model.reposritory.CourseRepository;
import org.example.shop1.model.reposritory.OrderRepository;
import org.example.shop1.model.reposritory.ProductRepository;
import org.example.shop1.model.reposritory.UserRepository;
import org.example.shop1.model.service.payment.MellatGatewayException;
import org.example.shop1.model.service.payment.MellatGatewayService;
import org.example.shop1.model.service.payment.PaymentRefNumberGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    // نرخِ مالیات بر ارزش افزوده — طبقِ تصمیمِ کارِ ۱ (docs/prompt-tech-chat-invoice-vat-and-shipping.md)
    private static final BigDecimal TAX_RATE = new BigDecimal("0.10");

    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final CourseRepository courseRepo;
    private final UserRepository userRepo;
    private final ShippingService shippingService;
    private final MellatGatewayService mellatGatewayService;
    private final PaymentRefNumberGenerator paymentRefNumberGenerator;

    public OrderService(OrderRepository orderRepo, ProductRepository productRepo, CourseRepository courseRepo,
                        UserRepository userRepo, ShippingService shippingService, MellatGatewayService mellatGatewayService,
                        PaymentRefNumberGenerator paymentRefNumberGenerator) {
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
        this.courseRepo = courseRepo;
        this.userRepo = userRepo;
        this.shippingService = shippingService;
        this.mellatGatewayService = mellatGatewayService;
        this.paymentRefNumberGenerator = paymentRefNumberGenerator;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userRepo.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("کاربر یافت نشد"));
        }
        // دیگر کاربر تستی ساختگی ساخته نمی‌شود؛ عملیات سفارش نیازمند ورود واقعی است
        throw new RuntimeException("برای انجام این عملیات ابتدا باید وارد شوید");
    }

    /*
       سفارش را پیدا می‌کند و مطمئن می‌شود متعلق به کاربر جاری است.
       ادمین به همه سفارش‌ها دسترسی دارد.
    */
    private Order getOwnedOrder(String orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("سفارش پیدا نشد"));

        User current = getCurrentUser();
        if (current.getRole() == Role.ADMIN) {
            return order;
        }

        if (order.getUser() == null || order.getUser().getId() == null
                || !order.getUser().getId().equals(current.getId())) {
            throw new RuntimeException("شما به این سفارش دسترسی ندارید");
        }
        return order;
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
        // ۱. پیدا کردن سفارش + بررسی مالکیت
        Order order = getOwnedOrder(id);

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
        Order order = getOwnedOrder(orderId);
        if (order.isFinalized()) throw new RuntimeException("امکان ویرایش سفارش نهایی شده وجود ندارد");
        return updateOrderItemsLogic(order, items);
    }

    private Order updateOrderItemsLogic(Order order, List<OrderRequestDto.CartItemDto> dtos) {
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal itemsTotal = BigDecimal.ZERO;

        for (OrderRequestDto.CartItemDto itemDto : dtos) {
            OrderItem orderItem;
            BigDecimal lineTotal;

            if (itemDto.getItemType() == OrderItemType.COURSE) {
                // 🔴 دوره از همان خطِ لولهٔ فروش رد می‌شود، ولی موجودیتِ جداست — طبقِ
                // docs/prompt-tech-chat-course-system.md. تخفیفِ محصول اینجا معنا ندارد.
                Course course = courseRepo.findById(itemDto.getProductId())
                        .orElseThrow(() -> new RuntimeException("دوره پیدا نشد"));
                if (!course.isActive()) {
                    throw new RuntimeException("این دوره دیگر برایِ ثبت‌نام باز نیست: " + course.getTitle());
                }
                BigDecimal price = course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO;
                String img = course.getBannerImage() != null && !course.getBannerImage().isBlank()
                        ? course.getBannerImage()
                        : (course.getImages() != null && !course.getImages().isEmpty() ? course.getImages().get(0) : null);

                orderItem = new OrderItem(course.getId(), course.getTitle(), img, price, itemDto.getQuantity());
                orderItem.setItemType(OrderItemType.COURSE);
                orderItem.setCourseMode(course.getMode());
                lineTotal = price.multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            } else {
                Product product = productRepo.findById(itemDto.getProductId()).orElseThrow(() -> new RuntimeException("محصول پیدا نشد"));

                BigDecimal finalPrice = product.getOnlinePrice() != null ? product.getOnlinePrice() : product.getPrice();
                if (product.getDiscountPercent() != null && product.getDiscountPercent() > 0) {
                    BigDecimal discount = finalPrice.multiply(BigDecimal.valueOf(product.getDiscountPercent())).divide(BigDecimal.valueOf(100));
                    finalPrice = finalPrice.subtract(discount);
                }

                String img = (product.getImages() != null && !product.getImages().isEmpty()) ? product.getImages().get(0) : null;
                orderItem = new OrderItem(product.getId(), product.getName(), img, finalPrice, itemDto.getQuantity());
                orderItem.setItemType(OrderItemType.PRODUCT);
                lineTotal = finalPrice.multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            }

            orderItems.add(orderItem);
            itemsTotal = itemsTotal.add(lineTotal);
        }

        order.setItems(orderItems);
        order.setItemsTotal(itemsTotal);
        applyTotals(order);
        return orderRepo.save(order);
    }

    /**
     * تکِ نقطهٔ محاسبهٔ مالیات و مبلغِ نهایی — از itemsTotalِ فعلیِ order می‌خواند.
     * هرجا itemsTotal بازمحاسبه می‌شود (ثبتِ اولیه، ویرایشِ اقلام، نهایی‌سازی) باید
     * همین متد صدا زده شود تا taxAmount/totalAmount هیچ‌وقت از itemsTotal عقب نیفتد.
     * <p>
     * ⚠️ هزینهٔ ارسال دیگر اینجا نیست — پس‌کرایه است، سایت هیچ مبلغی برایش حساب نمی‌کند.
     */
    private void applyTotals(Order order) {
        BigDecimal itemsTotal = order.getItemsTotal() != null ? order.getItemsTotal() : BigDecimal.ZERO;
        BigDecimal tax = itemsTotal.multiply(TAX_RATE).setScale(0, RoundingMode.HALF_UP);
        order.setTaxAmount(tax);
        order.setTotalAmount(itemsTotal.add(tax));
    }

    public List<ShippingOption> getShippingQuotes(String orderId, Address destination) {
        Order order = getOwnedOrder(orderId);
        double totalWeight = 0;
        for (OrderItem item : order.getItems()) {
            if (item.getItemType() == OrderItemType.COURSE) continue; // دوره وزن ندارد، پست نمی‌شود
            Product product = productRepo.findById(item.getProductId()).orElse(null);
            if (product != null && product.getWeight() != null) {
                totalWeight += (product.getWeight() * item.getQuantity());
            }
        }
        return shippingService.calculateOptions(destination, totalWeight);
    }

    @Transactional
    public Order finalizeOrder(String orderId, OrderRequestDto request) {
        Order order = getOwnedOrder(orderId);
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

            if (item.getItemType() == OrderItemType.COURSE) {
                Course course = courseRepo.findById(item.getProductId())
                        .orElseThrow(() -> new RuntimeException("دوره پیدا نشد: " + item.getProductName()));
                int remaining = course.getRemainingCapacity();
                if (!course.isActive() || remaining < item.getQuantity()) {
                    stockModified = true;
                    if (!course.isActive() || remaining <= 0) {
                        iterator.remove();
                    } else {
                        item.setQuantity(remaining);
                    }
                }
                continue;
            }

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
            applyTotals(order);
            orderRepo.save(order);

            // پرتاب خطا جهت باخبر کردن کلاینت و نمایش پاپ‌آپ تایید جدید
            throw new InsufficientStockException(order);
        }
        // =========================================================================

        order.setShippingAddress(request.getAddress());
        // فقط نامِ روشِ انتخابی پرسیست می‌شود؛ قیمتی از کلاینت گرفته/اعتماد نمی‌شود (پس‌کرایه است)
        order.setShippingMethod(request.getShippingMethod());
        applyTotals(order);

        order.setFinalized(true);
        return orderRepo.save(order);
    }

    public List<Order> getMyOrders() { return orderRepo.findByUserOrderByOrderDateDesc(getCurrentUser()); }
    public List<Order> getAllOrdersForAdmin() { return orderRepo.findAll(); }

    // با بررسی مالکیت: هر کاربر فقط سفارش خودش را می‌بیند (ادمین همه را)
    public Order getOrderById(String id) { return getOwnedOrder(id); }

    // ==========================================================
    // درگاهِ پرداختِ ملت — طبقِ docs/prompt-tech-chat-mellat-gateway.md
    // ==========================================================

    /**
     * شروعِ پرداخت: {@code paymentRefNumber}ِ یکتا می‌سازد، {@code bpPayRequest} می‌زند و
     * آدرسِ صفحه‌ی auto-submitِ داخلی را برمی‌گرداند (نه مستقیم آدرسِ بانک — چون خودِ
     * {@code RefId} باید قبلش رویِ سفارش ذخیره شود).
     * <p>
     * اگر {@code bpPayRequest} رد شود (ResCode≠۰)، وضعیتِ سفارش تغییر <b>نمی‌کند</b>
     * (PENDING_PAYMENT می‌ماند) — چون این شکست معمولاً موقتی/شبکه‌ای است و کاربر باید بتواند
     * دوباره تلاش کند؛ PAYMENT_FAILED فقط برایِ شکستِ واقعیِ گزارش‌شده از بانک (در callback) است.
     */
    @Transactional
    public Map<String, String> startMellatPayment(String orderId, String baseUrl) {
        Order order = getOwnedOrder(orderId);
        if (order.isPaid()) {
            throw new RuntimeException("این سفارش قبلاً پرداخت شده است");
        }
        if (!order.isFinalized()) {
            throw new RuntimeException("سفارش هنوز نهایی نشده است");
        }
        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("مبلغِ سفارش نامعتبر است");
        }
        if (!mellatGatewayService.isConfigured()) {
            // پیامِ روشن به‌جایِ خطایِ گنگِ SOAP — رایج در dev که کلیدهایِ ملت تنظیم نشده‌اند
            throw new RuntimeException("درگاهِ پرداخت هنوز پیکربندی نشده است (کلیدهایِ ملت در .env نیستند)");
        }

        long paymentRefNumber = paymentRefNumberGenerator.next();
        // ⚠️ واحدِ پروژه تومان است؛ ملت ریال می‌خواهد — دقیقاً ۱۰ برابر (رایج‌ترین باگِ اتصال)
        long amountRial = order.getTotalAmount().multiply(BigDecimal.TEN).longValueExact();
        String callBackUrl = baseUrl + "/api/orders/mellat-callback";

        MellatGatewayService.PayResult result;
        try {
            result = mellatGatewayService.pay(paymentRefNumber, amountRial, callBackUrl);
        } catch (MellatGatewayException e) {
            log.error("bpPayRequest برایِ سفارشِ {} با خطا مواجه شد: {}", order.getId(), e.getMessage());
            throw new RuntimeException("ارتباط با درگاهِ پرداخت برقرار نشد؛ لطفاً دوباره تلاش کنید");
        }

        if (!result.success()) {
            log.warn("bpPayRequest برایِ سفارشِ {} ردِ شد: ResCode={}", order.getId(), result.resCode());
            throw new RuntimeException("درگاهِ پرداخت درخواست را رد کرد (کدِ خطا: " + result.resCode() + ")");
        }

        order.setPaymentRefNumber(paymentRefNumber);
        order.setMellatRefId(result.refId());
        orderRepo.save(order);

        return Map.of("paymentUrl", "/api/orders/mellat-redirect/" + order.getId());
    }

    /** صفحه‌ی HTMLِ auto-submit که {@code RefId}ِ ذخیره‌شده را با POST به بانک می‌فرستد. */
    public String buildMellatRedirectHtml(String orderId) {
        Order order = getOwnedOrder(orderId);
        if (order.getMellatRefId() == null || order.getMellatRefId().isBlank()) {
            throw new RuntimeException("پرداخت برایِ این سفارش هنوز شروع نشده است");
        }
        String refId = order.getMellatRefId().replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
        return """
                <!DOCTYPE html>
                <html lang="fa" dir="rtl"><head><meta charset="UTF-8"><title>انتقال به درگاهِ پرداخت</title></head>
                <body onload="document.forms[0].submit()">
                <form method="POST" action="https://bpm.shaparak.ir/pgwchannel/startpay.mellat">
                <input type="hidden" name="RefId" value="%s"/>
                </form>
                <p style="font-family:Tahoma,Arial,sans-serif;text-align:center;margin-top:60px;color:#374151;">
                در حال انتقال به درگاهِ پرداختِ بانک ملت…</p>
                </body></html>
                """.formatted(refId);
    }

    /**
     * پردازشِ callbackِ ملت. این متد از سرورِ خودِ بانک صدا زده می‌شود (بدونِ سشنِ کاربر)، پس
     * نباید به احرازِ هویت/getOwnedOrder وابسته باشد.
     * <p>
     * 🔒 طبقِ نکتهٔ حیاتیِ پرامپت: هرگز فقط با دیدنِ این POST سفارش paid نمی‌شود. حتی اگر
     * ResCode=۰ باشد، یک {@code bpVerifyRequest}ِ سرور-به-سرورِ جدا (با یوزر/پسِ خودمان) لازم
     * است — چون این POST از مرورگرِ کاربر (که وسطش می‌تواند دست‌کاری شود) عبور می‌کند، نه یک
     * کانالِ مطمئن.
     *
     * @return مسیرِ ریدایرکتِ نهایی برایِ مرورگرِ کاربر
     */
    @Transactional
    public String handleMellatCallback(Map<String, String> params) {
        String resCode = firstNonBlank(params.get("ResCode"), params.get("resCode"));
        String saleOrderIdRaw = firstNonBlank(params.get("SaleOrderId"), params.get("saleOrderId"));
        String saleReferenceIdRaw = firstNonBlank(params.get("SaleReferenceId"), params.get("saleReferenceId"));

        if (saleOrderIdRaw == null) {
            log.error("callbackِ ملت بدونِ SaleOrderId رسید: {}", params);
            return "/profile?paymentError=1";
        }

        long paymentRefNumber;
        try {
            paymentRefNumber = Long.parseLong(saleOrderIdRaw.trim());
        } catch (NumberFormatException e) {
            log.error("SaleOrderId نامعتبر در callbackِ ملت: {}", saleOrderIdRaw);
            return "/profile?paymentError=1";
        }

        Order order = orderRepo.findByPaymentRefNumber(paymentRefNumber).orElse(null);
        if (order == null) {
            log.error("سفارشی با paymentRefNumber={} پیدا نشد (callbackِ ملت)", paymentRefNumber);
            return "/profile?paymentError=1";
        }

        // callbackِ تکراری (ریترایِ بانک/بازگشتِ مرورگر): اگر قبلاً کامل پردازش شده، دوباره
        // verify/settle نزن — پاسخِ دوباره‌ی ملت به یک تراکنشِ verify‌شده لزوماً "0" نیست.
        if (order.getStatus() == OrderStatus.PAID_PREPARING) {
            return "/profile?paid=1&order=" + order.getId();
        }

        if (!"0".equals(resCode)) {
            failOrder(order, "بانک ResCode=" + resCode + " برگرداند (پرداخت لغو/ناموفق شد)");
            return "/profile?paymentFailed=1&order=" + order.getId();
        }

        long saleReferenceId;
        try {
            saleReferenceId = Long.parseLong(saleReferenceIdRaw.trim());
        } catch (Exception e) {
            failOrder(order, "SaleReferenceIdِ نامعتبر از بانک: " + saleReferenceIdRaw);
            return "/profile?paymentFailed=1&order=" + order.getId();
        }

        // ۱. verify — تنها منبعِ معتبرِ تاییدِ تراکنش
        String verifyResCode;
        try {
            verifyResCode = mellatGatewayService.verify(paymentRefNumber, paymentRefNumber, saleReferenceId);
        } catch (MellatGatewayException e) {
            failOrder(order, "bpVerifyRequest ناموفق (خطایِ ارتباطی): " + e.getMessage());
            return "/profile?paymentFailed=1&order=" + order.getId();
        }
        if (!"0".equals(verifyResCode)) {
            failOrder(order, "bpVerifyRequest ناموفق (ResCode=" + verifyResCode + ")");
            return "/profile?paymentFailed=1&order=" + order.getId();
        }

        order.setMellatSaleReferenceId(String.valueOf(saleReferenceId));

        // ۲. settle — همان لحظه، طبقِ مستندِ ملت (وگرنه واریز به حسابِ فروشگاه انجام نمی‌شود)
        String settleResCode;
        try {
            settleResCode = mellatGatewayService.settle(paymentRefNumber, paymentRefNumber, saleReferenceId);
        } catch (MellatGatewayException e) {
            settleResCode = "ERR";
            log.error("bpSettleRequest برایِ سفارشِ {} با خطایِ ارتباطی مواجه شد: {}", order.getId(), e.getMessage());
        }

        // ResCode=45 یعنی «قبلاً settle شده» — طبقِ مستندِ ملت خطا نیست (رایج در ریترای)
        if (!"0".equals(settleResCode) && !"45".equals(settleResCode)) {
            log.error("bpSettleRequest ناموفق برایِ سفارشِ {}: ResCode={} — تلاش برایِ Reversal",
                    order.getId(), settleResCode);
            try {
                mellatGatewayService.reversal(paymentRefNumber, paymentRefNumber, saleReferenceId);
                failOrder(order, "Settle ناموفق (ResCode=" + settleResCode + ") — Reversal با موفقیت انجام شد");
            } catch (MellatGatewayException reversalError) {
                log.error("⚠️ Reversal هم برایِ سفارشِ {} شکست خورد — نیازِ پیگیریِ دستیِ فوری: {}",
                        order.getId(), reversalError.getMessage());
                failOrder(order, "Settle ناموفق (ResCode=" + settleResCode
                        + ") و Reversal هم شکست خورد — نیازِ پیگیریِ دستی با پشتیبانیِ بانک");
            }
            return "/profile?paymentFailed=1&order=" + order.getId();
        }

        orderRepo.save(order); // ذخیرهٔ mellatSaleReferenceId قبلِ تغییرِ وضعیت
        updateOrderStatus(order.getId(), OrderStatus.PAID_PREPARING); // کاهشِ موجودی هم همین‌جا انجام می‌شود
        return "/profile?paid=1&order=" + order.getId();
    }

    private void failOrder(Order order, String reason) {
        order.setStatus(OrderStatus.PAYMENT_FAILED);
        order.setPaymentFailureReason(reason);
        orderRepo.save(order);
        log.warn("سفارشِ {} به PAYMENT_FAILED رفت: {}", order.getId(), reason);
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }

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
                if (item.getItemType() == OrderItemType.COURSE) {
                    // به‌جایِ کاهشِ موجودیِ کالا، ظرفیتِ کلاس پر می‌شود (enrolledCount زیاد می‌شود)
                    Course course = courseRepo.findById(item.getProductId())
                            .orElseThrow(() -> new RuntimeException("دوره پیدا نشد: " + item.getProductName()));
                    if (course.getRemainingCapacity() < item.getQuantity()) {
                        throw new RuntimeException("متأسفانه در زمانِ پرداخت، ظرفیتِ دوره تکمیل شد: " + course.getTitle());
                    }
                    course.setEnrolledCount(course.getEnrolledCount() + item.getQuantity());
                    courseRepo.save(course);
                    continue;
                }

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

    /**
     * ثبتِ دستیِ لایسنس/لینکِ دانلودِ اسپات‌پلیر برایِ یک قلمِ دوره‌ی آنلاینِ داخلِ یک سفارش —
     * طبقِ تصمیمِ مالک (docs/prompt-tech-chat-course-system.md، بخشِ ۵) کاملاً دستی است،
     * بدونِ اتصال به APIِ اسپات‌پلیر. فقط ادمین (کنترلرِ رده‌بندی می‌کند).
     */
    @Transactional
    public Order setItemLicense(String orderId, String courseId, String licenseKey, String downloadLink) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("سفارش یافت نشد"));

        boolean found = false;
        for (OrderItem item : order.getItems()) {
            if (item.getItemType() == OrderItemType.COURSE && courseId.equals(item.getProductId())) {
                item.setLicenseKey(licenseKey);
                item.setDownloadLink(downloadLink);
                found = true;
            }
        }
        if (!found) {
            throw new RuntimeException("قلمِ دوره در این سفارش پیدا نشد");
        }
        return orderRepo.save(order);
    }
}