package org.example.shop1.controller;

import org.bson.Document;
import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.dto.CustomerDetailDto;
import org.example.shop1.model.dto.CustomerSummaryDto;
import org.example.shop1.model.entity.Order;
import org.example.shop1.model.entity.User;
import org.example.shop1.model.enums.Role;
import org.example.shop1.model.reposritory.CommentRepository;
import org.example.shop1.model.reposritory.OrderRepository;
import org.example.shop1.model.reposritory.UserRepository;
import org.example.shop1.model.reposritory.VisitorRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * «مدیریتِ مشتریان»ِ پنلِ فروشِ حضوری.
 * <p>
 * 🔴 <b>چرا یک کنترلرِ جدا و نه بازکردنِ مسیرهای ادمین:</b> همان صفحه در پنلِ ادمین
 * از {@code /api/users/admin/all}، {@code /api/orders/admin/**} و
 * {@code /api/v1/analytics/journey} می‌خورَد و هر سه شمارهٔ کاملِ مشتری را در پاسخ
 * دارند. بازکردنِ آن‌ها برایِ کارشناس یعنی شماره در دسترسِ اوست، هر چه جدول نشان
 * بدهد. پس شاخهٔ {@code /panel/} جداست و فقط DTOهای پوشانده بیرون می‌دهد.
 * <p>
 * 🔴 <b>جست‌وجو عمداً سمتِ کلاینت و فقط رویِ نام است.</b> جست‌وجوی سروری با شماره
 * پوشاندن را بی‌اثر می‌کرد: با چند پرس‌وجو می‌شد رقم‌های پوشانده را حدس زد.
 */
@RestController
@RequestMapping("/api/users/panel")
public class PanelCustomersController {

    /** سقفِ رویدادهایِ یک صفحه از سفرِ کاربر — همان عددِ تبِ رفتارِ ادمین. */
    private static final int JOURNEY_PAGE = 200;

    private final UserRepository userRepo;
    private final OrderRepository orderRepo;
    private final CommentRepository commentRepo;
    private final VisitorRepository visitorRepo;
    private final MongoTemplate mongo;

    public PanelCustomersController(UserRepository userRepo, OrderRepository orderRepo,
                                    CommentRepository commentRepo, VisitorRepository visitorRepo,
                                    MongoTemplate mongo) {
        this.userRepo = userRepo;
        this.orderRepo = orderRepo;
        this.commentRepo = commentRepo;
        this.visitorRepo = visitorRepo;
        this.mongo = mongo;
    }

    /**
     * فهرستِ مشتریان با تعدادِ سفارش.
     * <p>
     * ⚠️ تعداد با یک کوئریِ گروهی گرفته می‌شود، نه یک کوئری به‌ازای هر مشتری:
     * با چند هزار مشتری، حالتِ دوم صفحه را می‌خواباند.
     */
    @GetMapping("/customers")
    public List<CustomerSummaryDto> customers() {
        List<User> customers = userRepo.findByRole(Role.USER);

        // ⚠️ کلیدِ گروه دقیقاً "$user" است و نه "$user.$id": فیلدِ user در این
        // کلکشن یک ObjectIdِ ساده ذخیره شده، نه DBRef. با کلیدِ غلط هیچ خطایی
        // نمی‌داد — فقط تعدادِ همه صفر می‌شد، که شبیهِ «مشتری سفارش ندارد» است.
        Map<String, Integer> counts = new HashMap<>();
        for (Document d : mongo.getCollection("orders").aggregate(List.of(
                new Document("$group", new Document("_id", "$user")
                        .append("n", new Document("$sum", 1)))))) {
            Object id = d.get("_id");
            if (id != null) counts.merge(String.valueOf(id), d.getInteger("n", 0), Integer::sum);
        }

        return customers.stream().map(u -> {
            CustomerSummaryDto dto = CustomerSummaryDto.of(u);
            dto.setOrderCount(counts.getOrDefault(u.getId(), 0));
            return dto;
        }).toList();
    }

    /** پروندهٔ کاملِ یک مشتری — نشانی‌ها، فاکتورها و نظرها، همه با شمارهٔ پوشانده. */
    @GetMapping("/customers/{id}")
    public CustomerDetailDto customer(@PathVariable String id) {
        User u = requireCustomer(id);
        List<Order> orders = orderRepo.findByUserId(id);
        orders.sort(Comparator.comparing(Order::getOrderDate,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return new CustomerDetailDto(u, orders, commentRepo.findByUserId(id));
    }

    /**
     * سفرِ کاربر — همان خطِ زمانیِ تبِ رفتارِ ادمین، ولی محدود به یک مشتری.
     * <p>
     * ⚠️ فقط {@code userId} می‌گیرد و نه {@code anonId}: با anonIdِ دلخواه می‌شد
     * سفرِ هر بازدیدکننده‌ای را از این مسیر بیرون کشید، حتی کسی که مشتری نیست.
     */
    @GetMapping("/customers/{id}/journey")
    public Map<String, Object> journey(@PathVariable String id,
                                       @RequestParam(defaultValue = "0") int page) {
        requireCustomer(id);

        List<String> anonIds = new ArrayList<>();
        visitorRepo.findByUserId(id).forEach(v -> anonIds.add(v.getId()));
        if (anonIds.isEmpty()) {
            return Map.of("anonIds", List.of(), "events", List.of());
        }

        List<Document> events = mongo.getCollection("user_events")
                .find(new Document("anonId", new Document("$in", anonIds)))
                .sort(new Document("at", -1))
                .skip(Math.max(page, 0) * JOURNEY_PAGE).limit(JOURNEY_PAGE)
                .into(new ArrayList<>());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("anonIds", anonIds);
        out.put("events", events);
        return out;
    }

    /**
     * ⚠️ شرطِ نقش اینجا امنیتی است، نه آرایشی: بدونِ آن کارشناس می‌توانست شناسهٔ
     * یک ادمین را بدهد و پروندهٔ همکارش را باز کند.
     */
    private User requireCustomer(String id) {
        User u = userRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "مشتری یافت نشد"));
        if (u.getRole() != Role.USER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "این شناسه مشتری نیست");
        }
        return u;
    }

    /** پاسخِ خطا به‌شکلِ متنِ ساده، همان چیزی که بقیهٔ پنل انتظار دارد. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<String> onApiException(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(e.getMessage());
    }
}
