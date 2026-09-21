package org.example.shop1.controller;

import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.ActivityLog;
import org.example.shop1.model.entity.LegacyRedirect;
import org.example.shop1.model.reposritory.LegacyRedirectRepository;
import org.example.shop1.model.reposritory.ProductRepository;
import org.example.shop1.model.service.ActivityLogService;
import org.example.shop1.model.service.ArticleService;
import org.example.shop1.model.service.LegacyRedirectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * مدیریتِ ریدایرکتِ آدرس‌های قدیمی.
 * <p>
 * 🔴 واردکردن عمداً <b>CSVِ عام</b> می‌گیرد ({@code fromPath,toPath,note}) و نه قالبِ
 * فایلِ نگاشتِ یک فروشگاهِ مشخص. نامِ ستون‌های آن فایل و منطقِ «کدام اولویت وارد شود»
 * دانشِ همان فروشگاه است و جایش در هستهٔ اپ نیست؛ تبدیلش یک بار بیرون انجام می‌شود.
 */
@RestController
@RequestMapping("/api/v1/legacy-redirects")
public class LegacyRedirectAdminController {

    /** مسیرهای ثابتِ اپ که مقصدِ معتبرند بی‌آنکه سندی در دیتابیس داشته باشند. */
    private static final Set<String> STATIC_TARGETS =
            Set.of("/", "/shop", "/blog", "/learn", "/about", "/wimaxnear");

    private final LegacyRedirectRepository repo;
    private final LegacyRedirectService service;
    private final ArticleService articleService;
    private final ProductRepository productRepo;
    private final ActivityLogService activityLog;

    public LegacyRedirectAdminController(LegacyRedirectRepository repo, LegacyRedirectService service,
                                         ArticleService articleService, ProductRepository productRepo,
                                         ActivityLogService activityLog) {
        this.repo = repo;
        this.service = service;
        this.articleService = articleService;
        this.productRepo = productRepo;
        this.activityLog = activityLog;
    }

    @GetMapping
    public ResponseEntity<List<LegacyRedirect>> list() {
        return ResponseEntity.ok(repo.findAllByOrderByHitsDescCreatedAtDesc());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> addOne(@RequestBody Map<String, String> body) {
        String from = body.get("fromPath");
        String to = body.get("toPath");
        if (from == null || from.isBlank() || to == null || to.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "مسیرِ قدیمی و مقصد هر دو لازم‌اند");
        }
        // 🔴 ثبتِ تکی زنجیره را صاف می‌کند و حلقه را حذف، ولی واردکردنِ دسته‌ایِ CSV
        // عمداً همان رفتارِ خامِ قبلی را دارد: فایلِ نگاشتِ وردپرس یک عکسِ فوریِ
        // سازگار است و بازنویسیِ مقصدهایش وسطِ ایمپورت، نگاشت را از آنچه اپراتور
        // در فایل می‌بیند جدا می‌کند.
        Map<String, Object> result = service.addOneFlattened(from, to, body.get("note"), this::targetExists);
        if (Boolean.TRUE.equals(result.get("unchanged"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "مسیرِ قدیم و مقصد یکی‌اند — ثبتش فقط یک حلقه می‌سازد");
        }
        if ((int) result.get("created") + (int) result.get("updated") == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "ثبت نشد — مقصد وجود ندارد یا مسیر نامعتبر است");
        }
        log("افزودنِ ریدایرکتِ قدیمی", from, to);
        return ResponseEntity.ok(result);
    }

    /**
     * واردکردنِ دسته‌ای از CSV.
     * <p>
     * سطرِ اول اگر عنوان باشد نادیده گرفته می‌شود. ستون‌ها: مسیرِ قدیمی، مقصد، و
     * یادداشتِ اختیاری.
     */
    @PostMapping(value = "/import", consumes = {"text/csv", "text/plain"})
    public ResponseEntity<Map<String, Object>> importCsv(@RequestBody String csv) {
        List<LegacyRedirectService.MappingRow> rows = new ArrayList<>();
        for (String line : csv.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            String[] parts = trimmed.split(",", 3);
            if (parts.length < 2) continue;
            String from = parts[0].trim();
            // سطرِ عنوان
            if (from.equalsIgnoreCase("fromPath") || from.equalsIgnoreCase("from")) continue;
            rows.add(new LegacyRedirectService.MappingRow(
                    from, parts[1].trim(), parts.length > 2 ? parts[2].trim() : null));
        }
        if (rows.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "هیچ ردیفِ معتبری در فایل نبود");

        Map<String, Object> result = service.importRows(rows, this::targetExists);
        log("واردکردنِ دسته‌ایِ ریدایرکت", rows.size() + " ردیفِ ورودی",
                result.get("created") + " تازه، " + result.get("updated") + " به‌روز");
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        LegacyRedirect r = repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ریدایرکت پیدا نشد"));
        repo.deleteById(id);
        log("حذفِ ریدایرکتِ قدیمی", r.getFromPath(), null);
        return ResponseEntity.noContent().build();
    }

    /**
     * 🔴 آیا مقصد واقعاً باز می‌شود؟
     * <p>
     * مقاله باید <b>منتشرشده</b> باشد، نه فقط موجود: مقالهٔ منتشرنشده در
     * {@code /blog/{slug}} همان ۴۰۴ را می‌دهد، و ریدایرکت به ۴۰۴ از خودِ ۴۰۴ بدتر است.
     */
    private boolean targetExists(String toPath) {
        String path = toPath.split("[?#]", 2)[0];
        if (STATIC_TARGETS.contains(path)) return true;

        if (path.startsWith("/blog/hub/")) return true;
        if (path.startsWith("/blog/")) {
            String slug = path.substring("/blog/".length());
            return !slug.isBlank() && articleService.getPublishedBySlugOrId(slug).isPresent();
        }
        if (path.startsWith("/shop/product/")) {
            String slug = path.substring("/shop/product/".length()).split("/", 2)[0];
            return !slug.isBlank() && productRepo.findBySlug(slug).isPresent();
        }
        if (path.startsWith("/shop/category/") || path.startsWith("/shop/course/")) return true;
        return false;
    }

    private void log(String title, String oldValue, String newValue) {
        activityLog.record(ActivityLog.Action.ANALYTICS_EXPORT, ActivityLog.Source.MANUAL,
                ActivityLogService.ENTITY_ANALYTICS, null, title,
                "legacy-redirect", oldValue, newValue);
    }
}
