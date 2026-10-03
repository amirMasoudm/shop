package org.example.shop1.model.service;

import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.dto.RfqRequestDto;
import org.example.shop1.model.entity.*;
import org.example.shop1.model.enums.OrderStatus;
import org.example.shop1.model.enums.RfqStatus;
import org.example.shop1.model.enums.Role;
import org.example.shop1.model.reposritory.OrderRepository;
import org.example.shop1.model.reposritory.ProductRepository;
import org.example.shop1.model.reposritory.RfqRepository;
import org.example.shop1.model.reposritory.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RfqService {

    private static final Logger log = LoggerFactory.getLogger(RfqService.class);

    // تعداد روز اعتبار پیش‌فرض استعلام
    private static final int DEFAULT_VALID_DAYS = 7;

    private final RfqRepository rfqRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;
    private final OrderRepository orderRepo;
    private final StoreSettingsService settingsService;

    private final org.example.shop1.model.service.analytics.UserEventRecorder analytics;

    public RfqService(RfqRepository rfqRepo, ProductRepository productRepo, UserRepository userRepo,
                      OrderRepository orderRepo, StoreSettingsService settingsService,
                      org.example.shop1.model.service.analytics.UserEventRecorder analytics) {
        this.analytics = analytics;
        this.rfqRepo = rfqRepo;
        this.productRepo = productRepo;
        this.userRepo = userRepo;
        this.orderRepo = orderRepo;
        this.settingsService = settingsService;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userRepo.findByUsername(auth.getName())
                    .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "کاربر یافت نشد"));
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "برای این عملیات ابتدا باید وارد شوید");
    }

    // پیدا کردن استعلام + بررسی مالکیت (ادمین به همه دسترسی دارد) — الگوی OrderService.getOwnedOrder
    private Rfq getOwnedRfq(String id) {
        Rfq rfq = rfqRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "استعلام پیدا نشد"));
        User current = getCurrentUser();
        if (current.getRole() == Role.ADMIN) return rfq;
        if (rfq.getUser() == null || rfq.getUser().getId() == null
                || !rfq.getUser().getId().equals(current.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "شما به این استعلام دسترسی ندارید");
        }
        return rfq;
    }

    // آستانه‌ی فعال RFQ از تنظیمات فروشگاه (۰ یا null یعنی غیرفعال)
    public BigDecimal getThreshold() {
        BigDecimal t = settingsService.getSettings().getRfqThreshold();
        return t != null ? t : BigDecimal.ZERO;
    }

    private boolean rfqEnabled() {
        return getThreshold().compareTo(BigDecimal.ZERO) > 0;
    }

    // ======== کاربر ========

    @Transactional
    public Rfq createRfq(RfqRequestDto.CreateRequest request) {
        User user = getCurrentUser();

        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "سبد استعلام خالی است");
        }
        if (!rfqEnabled()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "امکان ثبت استعلام در حال حاضر فعال نیست");
        }

        List<RfqItem> items = new ArrayList<>();
        BigDecimal listTotal = BigDecimal.ZERO;

        for (RfqRequestDto.Line line : request.getItems()) {
            if (line.getProductId() == null || line.getQuantity() <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "قلم نامعتبر در سبد استعلام");
            }
            Product product = productRepo.findById(line.getProductId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "محصول یافت نشد"));

            // قیمت مرجع سروری (به قیمت فرانت اعتماد نمی‌کنیم)
            BigDecimal listUnit = product.getOnlinePrice() != null ? product.getOnlinePrice() : product.getPrice();
            if (listUnit == null) listUnit = BigDecimal.ZERO;
            if (product.getDiscountPercent() != null && product.getDiscountPercent() > 0) {
                BigDecimal discount = listUnit.multiply(BigDecimal.valueOf(product.getDiscountPercent()))
                        .divide(BigDecimal.valueOf(100));
                listUnit = listUnit.subtract(discount);
            }

            String img = (product.getImages() != null && !product.getImages().isEmpty())
                    ? product.getImages().get(0) : null;

            items.add(new RfqItem(product.getId(), product.getName(), img, line.getQuantity(),
                    listUnit, line.getProposedUnitPrice()));
            listTotal = listTotal.add(listUnit.multiply(BigDecimal.valueOf(line.getQuantity())));
        }

        // اعتبارسنجی سمت سرور: جمع مرجع باید از آستانه بگذرد
        if (listTotal.compareTo(getThreshold()) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "مبلغ سبد به حد نصاب استعلام نرسیده است");
        }

        Rfq rfq = new Rfq();
        rfq.setUser(user);
        rfq.setItems(items);
        rfq.setItemsListTotal(listTotal);
        rfq.setStatus(RfqStatus.PENDING);
        rfq.setCode("RFQ-" + (100000 + ThreadLocalRandom.current().nextInt(900000)));
        rfq.setCreatedAt(Instant.now());
        rfq.setExpiresAt(Instant.now().plus(DEFAULT_VALID_DAYS, ChronoUnit.DAYS));
        Rfq saved = rfqRepo.save(rfq);

        // اعلان بهترین‌تلاش (در MVP فقط لاگ؛ اتصال SMS واقعی فاز بعد)
        notifyBestEffort("ثبت استعلام جدید " + saved.getCode() + " توسط کاربر");
        // ثبتِ سروری — مرجع است، نه بیکنِ مرورگر که با هر افزونهٔ مسدودکننده گم می‌شود.
        analytics.record(org.example.shop1.model.enums.EventType.RFQ_SUBMIT,
                "RFQ", saved.getId(), saved.getCode());
        return saved;
    }

    public List<Rfq> getMyRfqs() {
        return rfqRepo.findByUserOrderByCreatedAtDesc(getCurrentUser());
    }

    public Rfq getRfqById(String id) {
        return getOwnedRfq(id);
    }

    // پیشنهاد قیمت کاربر (چانه‌زنی) — فقط صاحب استعلام
    @Transactional
    public Rfq userOffer(String id, RfqRequestDto.OfferRequest req) {
        Rfq rfq = getOwnedRfq(id);
        ensureNegotiable(rfq);
        if (req == null || req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "مبلغ پیشنهادی نامعتبر است");
        }
        rfq.getOffers().add(new RfqOffer("USER", req.getAmount(), req.getNote()));
        rfq.setStatus(RfqStatus.NEGOTIATING);
        return rfqRepo.save(rfq);
    }

    // تایید نهایی توسط کاربر → ساخت سفارش با مبلغ توافق‌شده
    @Transactional
    public Order acceptRfq(String id) {
        Rfq rfq = getOwnedRfq(id);
        if (rfq.getAdminTotalAmount() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "هنوز قیمت نهایی از سوی فروشگاه ثبت نشده است");
        }
        if (!EnumSet.of(RfqStatus.QUOTED,
                RfqStatus.NEGOTIATING).contains(rfq.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "این استعلام قابل تایید نیست");
        }

        Order order = new Order();
        order.setUser(rfq.getUser());
        order.setOrderDate(Instant.now());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setType("ONLINE");
        order.setOrderCode("ORD-" + (System.currentTimeMillis() % 1000000));

        List<OrderItem> orderItems = new ArrayList<>();
        for (RfqItem it : rfq.getItems()) {
            BigDecimal unit = it.getProposedUnitPrice() != null ? it.getProposedUnitPrice() : it.getListUnitPrice();
            orderItems.add(new OrderItem(it.getProductId(), it.getProductName(), it.getProductImage(),
                    unit, it.getQuantity()));
        }
        order.setItems(orderItems);
        // مبلغ مرجع اقلام و مبلغ نهایی = مبلغ توافق‌شده‌ی ادمین (هزینه ارسال در مسیر عادی checkout اضافه می‌شود)
        order.setItemsTotal(rfq.getAdminTotalAmount());
        order.setTotalAmount(rfq.getAdminTotalAmount());
        Order savedOrder = orderRepo.save(order);

        rfq.setStatus(RfqStatus.ACCEPTED);
        rfq.setLinkedOrderId(savedOrder.getId());
        rfqRepo.save(rfq);

        return savedOrder;
    }

    private void ensureNegotiable(Rfq rfq) {
        if (EnumSet.of(RfqStatus.ACCEPTED,
                RfqStatus.REJECTED,
                RfqStatus.EXPIRED).contains(rfq.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "این استعلام بسته شده و قابل تغییر نیست");
        }
    }

    // ======== ادمین ========

    public List<Rfq> getAllForAdmin() {
        return rfqRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Rfq adminQuote(String id, RfqRequestDto.OfferRequest req) {
        Rfq rfq = rfqRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "استعلام پیدا نشد"));
        ensureNegotiable(rfq);
        if (req == null || req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "مبلغ نامعتبر است");
        }
        rfq.setAdminTotalAmount(req.getAmount());
        rfq.getOffers().add(new RfqOffer("ADMIN", req.getAmount(), req.getNote()));
        rfq.setStatus(RfqStatus.QUOTED);
        Rfq saved = rfqRepo.save(rfq);
        notifyBestEffort("ثبت قیمت برای استعلام " + saved.getCode());
        return saved;
    }

    @Transactional
    public Rfq adminReject(String id) {
        Rfq rfq = rfqRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "استعلام پیدا نشد"));
        ensureNegotiable(rfq);
        rfq.setStatus(RfqStatus.REJECTED);
        return rfqRepo.save(rfq);
    }

    // اعلان بهترین‌تلاش؛ در MVP فقط لاگ می‌کند (اتصال SMS واقعی به فاز بعد موکول شد؛ بدون خطا)
    private void notifyBestEffort(String message) {
        try {
            log.info("[RFQ notify] {}", message);
        } catch (Exception ignored) {
        }
    }
}
