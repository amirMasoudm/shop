package org.example.shop1.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.shop1.config.SecurityUtils;
import org.example.shop1.model.entity.Category;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.reposritory.CategoryRepository;
import org.example.shop1.model.reposritory.ProductRepository;
import org.example.shop1.model.service.TorobTokenService;
import org.example.shop1.model.service.util.ProductUrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Torob Product API v3 — ترب خودش با JWT به این اندپوینت POST می‌زند (فیدِ XML نمی‌خواند).
 * مرجع: github.com/Torob/Torob-Sync
 * <p>
 * ⚠️ تا وقتی DNS/TLS سوییچ نشده و واحدِ قیمت (تومان یا هزارتومان) قطعی نشده،
 * این API نباید به ترب معرفی شود — قیمتِ اشتباه جلوی چشمِ کلِ بازار منتشر می‌شود.
 */
@RestController
public class TorobApiController {

    private static final Logger log = LoggerFactory.getLogger(TorobApiController.class);

    private static final int PAGE_SIZE = 100;
    private static final String API_VERSION = "torob_api_v3";
    private static final String GUARANTEE = "ضمانت اصالت کالا";

    // سقف‌هایِ مستنداتِ ترب
    private static final int MAX_TITLE = 500;
    private static final int MAX_SUBTITLE = 500;
    private static final int MAX_SHORT_DESC = 500;
    private static final int MAX_CATEGORY = 200;

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final TorobTokenService tokenService;

    public TorobApiController(ProductRepository productRepo, CategoryRepository categoryRepo,
                              TorobTokenService tokenService) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.tokenService = tokenService;
    }

    @PostMapping("/torob_api/v3/products")
    public ResponseEntity<?> products(@RequestBody(required = false) Map<String, Object> body,
                                      @RequestHeader(value = "X-Torob-Token", required = false) String token,
                                      HttpServletRequest request) {

        // ---------- احرازِ هویت ----------
        TorobTokenService.Result auth = tokenService.verify(token, request.getHeader("Host"));
        if (!auth.valid()) {
            log.warn("درخواستِ ردشدهٔ ترب: {}", auth.reason());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "unauthorized"));
        }

        // ---------- تشخیصِ حالتِ درخواست ----------
        // مستندات صریح است: هیچ مقدارِ پیش‌فرضی نگذار؛ بدنهٔ خالی/نامعتبر → ۴۰۰
        if (body == null || body.isEmpty()) {
            return badRequest("بدنهٔ درخواست خالی است");
        }

        boolean hasPage = body.containsKey("page");
        boolean hasUrls = body.containsKey("page_urls");
        boolean hasUniques = body.containsKey("page_uniques");

        if ((hasPage ? 1 : 0) + (hasUrls ? 1 : 0) + (hasUniques ? 1 : 0) != 1) {
            return badRequest("دقیقاً یکی از page / page_urls / page_uniques باید ارسال شود");
        }

        // همهٔ محصولاتِ واجدِ شرایط (بی‌قیمت‌ها حذف می‌شوند — پایین توضیح داده شده)
        List<Product> eligible = eligibleProducts();

        if (hasUniques) {
            List<String> ids = asStringList(body.get("page_uniques"));
            if (ids == null) return badRequest("page_uniques باید آرایه‌ای از رشته باشد");
            List<Product> found = eligible.stream().filter(p -> ids.contains(p.getId())).toList();
            // طبقِ مستندات، برایِ شناسهٔ ناموجود آرایهٔ خالی برمی‌گردد (نه ۴۰۴)
            return ResponseEntity.ok(buildResponse(found, 1, found.size(), 1, request));
        }

        if (hasUrls) {
            List<String> urls = asStringList(body.get("page_urls"));
            if (urls == null) return badRequest("page_urls باید آرایه‌ای از رشته باشد");
            String baseUrl = buildBaseUrl(request);
            List<Product> found = eligible.stream()
                    .filter(p -> urlMatches(p, baseUrl, urls))
                    .toList();
            return ResponseEntity.ok(buildResponse(found, 1, found.size(), 1, request));
        }

        // ---------- حالتِ صفحه‌بندی ----------
        int page;
        try {
            page = Integer.parseInt(String.valueOf(body.get("page")).trim());
        } catch (NumberFormatException e) {
            return badRequest("page باید عددِ صحیح باشد");
        }
        if (page < 1) {
            return badRequest("صفحه‌ها از ۱ شروع می‌شوند");
        }

        Object sortRaw = body.get("sort");
        String sort = sortRaw == null ? null : String.valueOf(sortRaw).trim();
        if (!"date_added_desc".equals(sort) && !"date_updated_desc".equals(sort)) {
            return badRequest("sort باید date_added_desc یا date_updated_desc باشد");
        }

        List<Product> sorted = new ArrayList<>(eligible);
        if ("date_updated_desc".equals(sort)) {
            // fallback به createdAt، وگرنه رکوردِ بدونِ updatedAt سورت را می‌شکند
            sorted.sort(Comparator.comparing(
                    (Product p) -> effectiveUpdatedAt(p), Comparator.nullsLast(Comparator.reverseOrder())));
        } else {
            sorted.sort(Comparator.comparing(
                    (Product p) -> p.getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder())));
        }

        int total = sorted.size();
        int maxPages = total == 0 ? 0 : (int) Math.ceil(total / (double) PAGE_SIZE);
        int from = (page - 1) * PAGE_SIZE;
        List<Product> slice = from >= total ? List.of() : sorted.subList(from, Math.min(from + PAGE_SIZE, total));

        return ResponseEntity.ok(buildResponse(slice, page, total, maxPages, request));
    }

    // ==========================================================
    // انتخابِ محصولاتِ واجدِ شرایط
    // ==========================================================

    /**
     * محصولاتی که به ترب فرستاده می‌شوند.
     * <p>
     * محصولِ بدونِ قیمتِ واقعی عمداً حذف می‌شود: طبقِ مستنداتِ ترب،
     * {@code current_price: 0} به‌همراهِ {@code availability: true} یعنی «کالای رایگان» —
     * یعنی محصولِ بی‌قیمت به‌صورتِ رایگان در کلِ بازار فهرست می‌شد.
     * محصولِ بی‌عکس هم کنار گذاشته می‌شود چون {@code image_links} الزامی است.
     * <p>
     * ناموجودها می‌مانند (با {@code availability:false}) — هم‌راستا با چارچوبِ ناموجودیِ پروژه.
     */
    private List<Product> eligibleProducts() {
        List<Product> all = productRepo.findAll();
        List<Product> result = new ArrayList<>();
        int skippedNoPrice = 0, skippedNoImage = 0;

        for (Product p : all) {
            if (toToman(p.getOnlinePrice()) <= 0) { skippedNoPrice++; continue; }
            if (p.getImages() == null || p.getImages().stream().noneMatch(this::notBlank)) {
                skippedNoImage++; continue;
            }
            result.add(p);
        }

        if (skippedNoPrice > 0 || skippedNoImage > 0) {
            log.info("ترب: {} محصول بدونِ قیمت و {} محصول بدونِ عکس از خروجی کنار گذاشته شد (از {} محصول)",
                    skippedNoPrice, skippedNoImage, all.size());
        }
        return result;
    }

    // ==========================================================
    // ساختِ پاسخ
    // ==========================================================

    private Map<String, Object> buildResponse(List<Product> products, int currentPage,
                                              int total, int maxPages, HttpServletRequest request) {
        String baseUrl = buildBaseUrl(request);
        Map<String, String> categoryNames = categoryNameMap();

        List<Map<String, Object>> items = new ArrayList<>();
        for (Product p : products) {
            items.add(toTorobProduct(p, baseUrl, categoryNames));
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("api_version", API_VERSION);
        resp.put("current_page", currentPage);
        resp.put("total", total);
        resp.put("max_pages", maxPages);
        resp.put("products", items);
        return resp;
    }

    private Map<String, Object> toTorobProduct(Product p, String baseUrl, Map<String, String> categoryNames) {
        Map<String, Object> m = new LinkedHashMap<>();

        // page_unique باید همیشه ثابت بماند → id، نه اسلاگ (اسلاگ ممکن است تغییر کند)
        m.put("page_unique", p.getId());
        m.put("page_url", baseUrl + ProductUrlUtil.hybridPathEncoded(p));
        m.put("title", truncate(p.getName(), MAX_TITLE));

        String subtitle = truncate(p.getSeoTitle(), MAX_SUBTITLE);
        if (notBlank(subtitle)) m.put("subtitle", subtitle);

        long current = toToman(p.getOnlinePrice());
        m.put("current_price", current);

        // old_price فقط وقتی واقعاً تخفیفی وجود دارد
        long old = toToman(p.getPrice());
        if (old > current) m.put("old_price", old);

        m.put("availability", p.getStock() != null && p.getStock() > 0);

        String catName = categoryNames.get(p.getCategoryId());
        if (notBlank(catName)) m.put("category_name", truncate(catName, MAX_CATEGORY));

        List<String> images = new ArrayList<>();
        if (p.getImages() != null) {
            for (String img : p.getImages()) {
                if (notBlank(img)) images.add(absoluteUrl(img, baseUrl));
            }
        }
        m.put("image_links", images);

        String shortDesc = notBlank(p.getSeoDescription())
                ? p.getSeoDescription()
                : SecurityUtils.clean(p.getDescription()); // متنِ خالص — HTMLِ احتمالی پاک می‌شود
        if (notBlank(shortDesc)) m.put("short_desc", truncate(shortDesc.trim(), MAX_SHORT_DESC));

        // ترب dict می‌خواهد؛ خالی باید {} باشد نه null
        m.put("spec", p.getSpecifications() == null ? Map.of() : p.getSpecifications());
        m.put("guarantee", GUARANTEE);

        Instant created = p.getCreatedAt();
        if (created != null) m.put("date_added", created.toString());
        Instant updated = effectiveUpdatedAt(p);
        if (updated != null) m.put("date_updated", updated.toString());

        // seller_name/seller_city عمداً فرستاده نمی‌شوند — مالِ مارکت‌پلیس‌هاست
        return m;
    }

    // ==========================================================
    // کمکی‌ها
    // ==========================================================

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "bad_request");
        body.put("message", message);
        return ResponseEntity.badRequest().body(body);
    }

    /** قیمت به عددِ صحیحِ تومان. مقدارِ نال/منفی → صفر (که در فیلتر حذف می‌شود). */
    private long toToman(BigDecimal value) {
        if (value == null) return 0;
        long v = value.setScale(0, java.math.RoundingMode.HALF_UP).longValue();
        return Math.max(v, 0);
    }

    private Instant effectiveUpdatedAt(Product p) {
        return p.getUpdatedAt() != null ? p.getUpdatedAt() : p.getCreatedAt();
    }

    private Map<String, String> categoryNameMap() {
        Map<String, String> map = new HashMap<>();
        for (Category c : categoryRepo.findAll()) {
            map.put(c.getId(), c.getName());
        }
        return map;
    }

    private boolean urlMatches(Product p, String baseUrl, List<String> urls) {
        String encoded = baseUrl + ProductUrlUtil.hybridPathEncoded(p);
        String raw = baseUrl + ProductUrlUtil.hybridPath(p);
        for (String u : urls) {
            if (u == null) continue;
            String candidate = u.trim();
            String stripped = stripTrailingSlash(candidate);
            // هم فرمِ encode شده هم خام پذیرفته می‌شود (ترب ممکن است هرکدام را بفرستد)
            if (stripped.equalsIgnoreCase(stripTrailingSlash(encoded))
                    || stripped.equals(stripTrailingSlash(raw))
                    || stripped.equalsIgnoreCase(stripTrailingSlash(decodeSafe(encoded)))) {
                return true;
            }
        }
        return false;
    }

    private String decodeSafe(String s) {
        try {
            return java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private String stripTrailingSlash(String s) {
        return s != null && s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private String absoluteUrl(String path, String baseUrl) {
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        return baseUrl + (path.startsWith("/") ? path : "/" + path);
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /** hostname/scheme از خودِ ریکوئست — با canonicalِ سایت هم‌خوان می‌ماند. */
    private String buildBaseUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() +
                (request.getServerPort() == 80 || request.getServerPort() == 443
                        ? "" : ":" + request.getServerPort());
    }

    private List<String> asStringList(Object raw) {
        if (!(raw instanceof List<?> list)) return null;
        List<String> out = new ArrayList<>();
        for (Object o : list) {
            if (o == null) return null;
            out.add(String.valueOf(o));
        }
        return out;
    }
}
