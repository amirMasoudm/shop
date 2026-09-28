package org.example.shop1.model.service;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.reposritory.CategoryRepository;
import org.example.shop1.model.reposritory.ProductRepository;
import org.example.shop1.model.service.util.ProductUrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * آخرین فرصتِ یک آدرس پیش از ۴۰۴ — نجاتِ آدرس‌های سایتِ وردپرسیِ قدیمی.
 * <p>
 * 🔴 <b>چرا لازم شد:</b> سرچ کنسول نشان داد از ۴۳۳ صفحهٔ ایندکس‌شده فقط ۱۵۴ آدرسِ
 * اپِ ماست؛ بقیه آدرس‌های سایتِ قدیمی‌اند و جدولِ ریدایرکت فقط ۴۹ ردیف دارد. ولی
 * ایمپورتِ وردپرس اسلاگ‌ها را عیناً نگه داشته، پس آدرسِ قدیمی معمولاً <b>همان اسلاگِ
 * مقالهٔ امروز است، فقط بدونِ پیشوندِ {@code /blog/}</b>. یک قاعده صدها آدرس را
 * برمی‌گرداند، بی‌آنکه ردیفی به دیتابیس اضافه شود.
 * <p>
 * <b>قاعده‌ها</b>، به همین ترتیب امتحان می‌شوند و اولین مقصدِ معتبر برنده است:
 * <ol>
 *   <li><b>اسلشِ پایانی</b> — تطبیقِ اسلشِ پایانی در اسپرینگ‌بوت ۳ پیش‌فرض خاموش شد،
 *       ولی سایتِ وردپرسی همه‌جا اسلش داشت. اگر نسخهٔ بی‌اسلش هندلر دارد، همان.</li>
 *   <li><b>مسیرِ تک‌بخشی</b> — {@code /{x}} که {@code x} اسلاگِ مقاله‌ای است.</li>
 *   <li><b>پرمالینکِ تاریخ‌دار</b> — {@code /{YYYY}/{MM}/{DD}/{x}} یا
 *       {@code /{YYYY}/{MM}/{x}}، خواهرِ قاعدهٔ قبلی: همان اسلاگ، با پیشوندِ تاریخِ
 *       وردپرس. این لینک‌ها هنوز داخلِ متنِ بعضی مقالات مانده‌اند.</li>
 *   <li><b>ریشهٔ فروشگاهِ ووکامرس</b> — دقیقاً {@code /product} به خودِ فروشگاه.</li>
 *   <li><b>{@code /product/{x}}</b> — اگر محصولی با آن اسلاگ باشد.</li>
 *   <li><b>{@code /product-category/{…}}</b> — اگر آخرین بخشْ اسلاگِ یک دسته باشد.</li>
 * </ol>
 * <p>
 * ⚠️ <b>ردیفِ صریحِ جدول همیشه بر این قاعده‌ها مقدم است</b>، و این نیازی به کدِ
 * ترتیب‌دهنده ندارد: {@code LegacyRedirectFilter} پیش از مسیریابیِ اسپرینگ اجرا
 * می‌شود، پس آدرسی که ردیف دارد اصلاً به ۴۰۴ و به این کلاس نمی‌رسد. تنها استثنا
 * مسیرهای «دیرحل‌شونده»اند که خودِ فیلتر عمداً ردشان می‌کند.
 * <p>
 * ⚠️ <b>چرا در nginx حل نشد:</b> یک {@code rewrite} کلیِ اسلش، هر ۴۹ ریدایرکتِ موجود
 * را دوپرشی می‌کرد — چون {@code normalize()} خودش اسلشِ پایانی را برمی‌دارد و آن ۴۹
 * تا همین حالا با اسلش هم یک‌پرشی جواب می‌دهند.
 * <p>
 * ⚠️ قاعدهٔ ۲ مقصد را از زنجیرهٔ {@link LegacyRedirect} هم رد می‌کند: اسلاگِ خیلی از
 * مقاله‌ها عوض شده، و رفتن به {@code /blog/{x}}ی که خودش ۳۰۱ می‌دهد یعنی دو پرش.
 */
@Service
public class LegacyPathFallback {

    private static final Logger log = LoggerFactory.getLogger(LegacyPathFallback.class);

    /** مسیرهایی که این فالبک هرگز نباید به آن‌ها دست بزند. */
    private static final List<String> BLOCKED_PREFIXES = List.of(
            "/api", "/l", "/error", "/actuator",
            "/uploads", "/css", "/js", "/img", "/images", "/fonts", "/fragments");

    /** پسوندهایی که یعنی «فایل»، نه صفحه. {@code .xml} و {@code .txt} هم نقشهٔ سایت و robots را می‌پوشانند. */
    private static final List<String> BLOCKED_SUFFIXES = List.of(
            ".html", ".css", ".js", ".map", ".json", ".xml", ".txt", ".pdf",
            ".png", ".jpg", ".jpeg", ".gif", ".svg", ".ico", ".webp", ".avif",
            ".woff", ".woff2", ".ttf", ".eot");

    /** سقفِ دنبال‌کردنِ زنجیرهٔ ریدایرکت. زنجیره در زمانِ نوشتن صاف می‌شود، این فقط بیمه است. */
    private static final int MAX_HOPS = 5;

    /** ریشهٔ فروشگاه — مقصدِ قاعدهٔ ۵. وجودش پیش از استفاده از جدولِ مسیریابی پرسیده می‌شود. */
    private static final String SHOP_ROOT = "/shop";

    private final ArticleService articleService;
    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final LegacyRedirectService legacyRedirects;
    /** با ObjectProvider تا وابستگیِ حلقوی با خودِ مسیریابی درست نشود. */
    private final ObjectProvider<RequestMappingHandlerMapping> mappings;

    public LegacyPathFallback(ArticleService articleService, ProductRepository productRepo,
                              CategoryRepository categoryRepo,
                              LegacyRedirectService legacyRedirects,
                              ObjectProvider<RequestMappingHandlerMapping> mappings) {
        this.articleService = articleService;
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.legacyRedirects = legacyRedirects;
        this.mappings = mappings;
    }

    /**
     * مقصدِ نجات برایِ این درخواست، یا {@code null} اگر باید ۴۰۴ِ واقعی بماند.
     *
     * @param request درخواستِ در حالِ ERROR dispatch — مسیرِ اصلی از صفتِ
     *                {@code jakarta.servlet.error.request_uri} خوانده می‌شود، نه از
     *                {@code getRequestURI()} که این‌جا {@code /error} است.
     */
    public String resolve(HttpServletRequest request) {
        try {
            if (!isReadRequest(request)) return null;
            Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
            if (status instanceof Integer code && code != 404) return null;

            String raw = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
            if (raw == null || raw.isBlank()) return null;

            String path = cleanPath(raw);
            if (path == null) return null;

            // شکلی که مرورگر واقعاً خواسته (دیکدشده، با اسلشِ پایانی) — قاعدهٔ ۱ به آن
            // نیاز دارد تا بفهمد اصلاً چیزی برایِ اصلاح وجود داشته یا نه.
            return targetFor(path, decodeAndStrip(raw));
        } catch (Exception e) {
            // نجاتِ آدرس هرگز نباید خودش خطا بسازد؛ ۴۰۴ِ عادی بدترین حالتش است
            log.debug("فالبکِ مسیرِ قدیمی ناموفق بود: {}", e.toString());
            return null;
        }
    }

    private boolean isReadRequest(HttpServletRequest request) {
        Object m = request.getAttribute("jakarta.servlet.error.method");
        String method = (m instanceof String s) ? s : request.getMethod();
        return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
    }

    /**
     * حذفِ کوئری/فرگمنت و دیکد — یعنی همان چیزی که مرورگر واقعاً درخواست کرده،
     * <b>با</b> اسلشِ پایانی.
     * <p>
     * ⚠️ دیکد عمداً اینجاست و نه بعدتر: مقایسهٔ «آیا مسیر اصلاح شد؟» باید روی شکلِ
     * دیکدشده انجام شود. اگر با رشتهٔ خام مقایسه شود، هر آدرسِ فارسیِ درصد-کدشده
     * «عوض‌شده» حساب می‌شود و همان حلقهٔ ۳۰۱ برمی‌گردد.
     */
    private String decodeAndStrip(String rawUri) {
        String p = rawUri;
        int cut = p.indexOf('?');
        if (cut >= 0) p = p.substring(0, cut);
        cut = p.indexOf('#');
        if (cut >= 0) p = p.substring(0, cut);
        try {
            return URLDecoder.decode(p, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // مسیرِ درصد-کدشدهٔ خراب: خام می‌ماند و در نهایت ۴۰۴ می‌گیرد
            return p;
        }
    }

    /** دیکد، حذفِ کوئری و اسلشِ پایانی، و ردِ مسیرهای ممنوع. {@code null} یعنی رد. */
    private String cleanPath(String rawUri) {
        String p = decodeAndStrip(rawUri);
        while (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
        if (p.isEmpty() || !p.startsWith("/") || p.equals("/")) return null;
        if (p.contains("..")) return null;

        String lower = p.toLowerCase(java.util.Locale.ROOT);
        for (String s : BLOCKED_SUFFIXES) if (lower.endsWith(s)) return null;
        for (String prefix : BLOCKED_PREFIXES) {
            if (lower.equals(prefix) || lower.startsWith(prefix + "/")) return null;
        }
        return p;
    }

    /**
     * @param requested شکلِ دیکدشدهٔ چیزی که مرورگر خواست (با اسلشِ پایانی). قاعدهٔ ۱
     *                  بدونِ آن نمی‌داند اصلاً چیزی برایِ اصلاح وجود داشته یا نه.
     */
    private String targetFor(String path, String requested) {
        // قاعدهٔ ۱ — همین مسیر بدونِ اسلشِ پایانی هندلرِ واقعی دارد.
        //
        // 🔴 شرطِ «cleanPath واقعاً چیزی را اصلاح کرده باشد» حیاتی است و نبودنش یک
        // حلقهٔ ۳۰۱ِ بی‌پایان می‌ساخت: hasGetHandler با *الگو* تطبیق می‌دهد، پس
        // «/blog/هرچه» به الگویِ /blog/{slug} می‌خورد حتی وقتی آن مقاله وجود ندارد.
        // آن‌وقت مقصد دقیقاً خودِ همان مسیر می‌شد و مرورگر تا ابد ۳۰۱ می‌گرفت
        // (سنجشِ پراد: /blog/nabashad-xyz، /shop/product/nabashad-xyz و…).
        //
        // ⚠️ راهِ درست همین است، نه گاردِ سراسریِ «مقصد = مبدأ»: آن نسخه قاعدهٔ ۱ را
        // می‌کشت، چون دلیلِ وجودیِ این قاعده دقیقاً همان حالتی است که مقصد با مسیرِ
        // تمیزشده یکی است و فقط اسلشِ پایانی فرق دارد.
        if (!path.equals(requested) && hasGetHandler(path)) {
            log.info("فالبکِ ۴۰۴ [اسلشِ پایانی]: {} → {}", requested, path);
            return path;
        }

        String[] segments = path.substring(1).split("/");

        // قاعدهٔ ۲ — مسیرِ تک‌بخشی که اسلاگِ یک مقاله است
        if (segments.length == 1) {
            String target = articleTarget(segments[0], path, "اسلاگِ مقاله");
            if (target != null) return target;
        }

        // قاعدهٔ ۲-ب — پرمالینکِ تاریخ‌دارِ وردپرس: /{YYYY}/{MM}/{DD}/{x} یا /{YYYY}/{MM}/{x}.
        // عمداً از همان کمکیِ قاعدهٔ ۲ می‌رود تا اسلاگِ عوض‌شده هم از زنجیرهٔ ریدایرکت
        // رد شود و یک‌پرشی بماند، نه دوپرشی.
        String datedSlug = wordpressDatedSlug(segments);
        if (datedSlug != null) {
            String target = articleTarget(datedSlug, path, "پرمالینکِ وردپرس");
            if (target != null) return target;
        }

        // قاعدهٔ ۵ — ریشهٔ فروشگاهِ ووکامرس: /product/ (و /product) به خودِ فروشگاه.
        //
        // ⚠️ عمداً «دقیقاً یک بخش» است و نه پیشوند: /product/{x} کارِ قاعدهٔ ۳ است و
        // نباید اینجا بلعیده شود. چون اینجا طولِ ۱ لازم است و آنجا طولِ ۲، تداخلی
        // ممکن نیست و ترتیبشان هم اهمیتی ندارد.
        //
        // ⚠️ بعد از قاعدهٔ ۲ می‌آید: اگر روزی مقاله‌ای واقعاً اسلاگِ «product» داشته
        // باشد، صفحهٔ زندهٔ خودش مقدم است — همان قاعدهٔ همیشگیِ این فالبک.
        if (segments.length == 1 && "product".equalsIgnoreCase(segments[0]) && hasGetHandler(SHOP_ROOT)) {
            log.info("فالبکِ ۴۰۴ [ریشهٔ فروشگاهِ ووکامرس]: {} → {}", path, SHOP_ROOT);
            return SHOP_ROOT;
        }

        // قاعدهٔ ۳ — /product/{x}
        if (segments.length == 2 && "product".equalsIgnoreCase(segments[0])) {
            Optional<Product> p = productRepo.findBySlug(segments[1]);
            if (p.isPresent()) {
                String hybrid = ProductUrlUtil.hybridPath(p.get());
                log.info("فالبکِ ۴۰۴ [اسلاگِ محصول]: {} → {}", path, hybrid);
                return hybrid;
            }
        }

        // قاعدهٔ ۴ — /product-category/{…}
        //
        // آخرین بخش برداشته می‌شود چون ووکامرس دستهٔ تودرتو را با کلِ مسیرِ والد
        // می‌نوشت ({@code /product-category/mikrotik-products/mikrotik-switch/})
        // ولی دستهٔ ما تخت است و فقط اسلاگِ خودش را دارد.
        //
        // ⚠️ این قاعده فقط آن‌هایی را می‌گیرد که اسلاگشان اتفاقاً یکی است. بیشترِ
        // اسلاگ‌های وردپرسی فرق دارند («انجنیوس-engenius» در برابرِ «engenius»)، و
        // آن‌ها ردیفِ صریح لازم دارند — که چون فیلتر پیش از مسیریابی اجرا می‌شود،
        // خودبه‌خود بر این قاعده مقدم است.
        if (segments.length >= 2 && "product-category".equalsIgnoreCase(segments[0])) {
            String slug = segments[segments.length - 1];
            if (!slug.isEmpty() && categoryRepo.findBySlug(slug).filter(c -> !isWarehouse(c)).isPresent()) {
                String target = "/shop/category/" + slug;
                log.info("فالبکِ ۴۰۴ [دستهٔ ووکامرس]: {} → {}", path, target);
                return target;
            }
        }
        return null;
    }

    /**
     * دستهٔ انبار عمداً از قاعدهٔ ۴ بیرون است.
     * <p>
     * این‌ها تاکسونومیِ داخلیِ انبارند، نه ویترین — و به همین دلیل خودِ
     * {@code sitemap.xml} هم بیرونشان می‌گذارد. فرستادنِ ترافیکِ گوگل با یک ۳۰۱ به
     * صفحه‌ای که عمداً در نقشهٔ سایت نیست، دو تصمیمِ ناسازگار می‌شد.
     */
    private static boolean isWarehouse(org.example.shop1.model.entity.Category c) {
        return c.getType() != null && "WAREHOUSE".equalsIgnoreCase(c.getType());
    }

    /**
     * مقصدِ یک اسلاگِ مقاله: خودِ مقاله اگر منتشر شده، وگرنه مقصدِ نهاییِ زنجیرهٔ
     * ریدایرکت (اسلاگ عوض شده)، وگرنه {@code null}.
     */
    private String articleTarget(String slug, String path, String rule) {
        String blogPath = "/blog/" + slug;
        if (articleService.getPublishedBySlugOrId(slug).isPresent()) {
            log.info("فالبکِ ۴۰۴ [{}]: {} → {}", rule, path, blogPath);
            return blogPath;
        }
        // اسلاگ عوض شده: مقصدِ نهاییِ زنجیره، نه خودِ /blog/{x} که ۳۰۱ می‌دهد
        String chained = followRedirects(blogPath);
        // 🔴 گاردِ حلقه فقط همین‌جا لازم است، و عمداً سراسری نیست. قاعده‌های دیگر
        // مقصدی می‌دهند که وجودش <b>اثبات شده</b> (هندلرِ واقعی، مقالهٔ منتشرشده،
        // محصولِ موجود) پس نمی‌توانند به خودشان برگردند. ولی مقصدِ زنجیره هرچه
        // باشد از دیتابیس می‌آید و می‌تواند به همین مسیر اشاره کند —
        // «/blog/x → /x» یعنی ۳۰۱ِ بی‌پایان.
        //
        // ⚠️ نسخهٔ اولِ این گارد سراسری بود و قاعدهٔ ۱ را می‌کشت: آن‌جا مقصد همان
        // مسیرِ بدونِ اسلش است و normalize هم اسلشِ پایانی را برمی‌دارد، پس هر
        // «/shop/» حلقه تشخیص داده می‌شد.
        if (chained != null
                && !LegacyRedirectService.normalize(chained).equals(LegacyRedirectService.normalize(path))) {
            log.info("فالبکِ ۴۰۴ [{}، اسلاگِ تغییریافته]: {} → {}", rule, path, chained);
            return chained;
        }
        return null;
    }

    /**
     * اسلاگِ پرمالینکِ تاریخ‌دارِ وردپرس، یا {@code null} اگر مسیر از این جنس نیست.
     * <p>
     * ⚠️ سال و ماه و روز باید <b>تاریخِ معنادار</b> باشند، نه فقط عدد: بدونِ این، هر
     * مسیرِ چهاربخشی‌ای که سه بخشِ اولش شبیهِ عدد است (مثلاً {@code /2025/13/99/x})
     * به این قاعده می‌افتاد و یک کوئریِ بی‌مورد می‌خورد.
     * <p>
     * ⚠️ اسلاگِ تمام‌عددی رد می‌شود: {@code /2025/05/01} بایگانیِ روزِ اول است، نه
     * نوشته‌ای به اسلاگِ «01». خودِ وردپرس هم به همین دلیل اسلاگِ عددی نمی‌سازد.
     */
    private String wordpressDatedSlug(String[] segments) {
        if (segments.length != 3 && segments.length != 4) return null;
        if (!inRange(segments[0], 4, 4, 1990, 2100)) return null;   // سال
        if (!inRange(segments[1], 1, 2, 1, 12)) return null;        // ماه
        if (segments.length == 4 && !inRange(segments[2], 1, 2, 1, 31)) return null;  // روز
        String slug = segments[segments.length - 1];
        if (slug.isEmpty() || slug.chars().allMatch(Character::isDigit)) return null;
        return slug;
    }

    /** رشتهٔ تمام‌رقمی با طولِ مجاز و مقدارِ داخلِ بازه. */
    private static boolean inRange(String s, int minLen, int maxLen, int min, int max) {
        if (s.length() < minLen || s.length() > maxLen) return false;
        if (!s.chars().allMatch(c -> c >= '0' && c <= '9')) return false;
        int v = Integer.parseInt(s);
        return v >= min && v <= max;
    }

    /** مقصدِ نهاییِ زنجیرهٔ ریدایرکت، یا {@code null} اگر اصلاً ریدایرکتی نبود. */
    private String followRedirects(String startPath) {
        String cursor = startPath;
        String last = null;
        for (int hop = 0; hop < MAX_HOPS; hop++) {
            Optional<org.example.shop1.model.entity.LegacyRedirect> hit = legacyRedirects.resolve(cursor);
            if (hit.isEmpty()) break;
            String next = hit.get().getToPath();
            if (next == null || next.equals(cursor)) break;
            last = next;
            cursor = next;
        }
        return last;
    }

    /**
     * آیا کنترلری واقعاً این مسیر را با GET جواب می‌دهد؟
     * <p>
     * به‌جایِ فهرستِ دستیِ مسیرها از خودِ جدولِ مسیریابی پرسیده می‌شود — هم مسیرِ تازه‌ای
     * که فردا اضافه شود خودکار پوشش می‌گیرد، هم هیچ نامِ مخصوصِ این فروشگاه در کد
     * هاردکد نمی‌شود.
     */
    private boolean hasGetHandler(String path) {
        RequestMappingHandlerMapping m = mappings.getIfAvailable();
        if (m == null) return false;
        PathContainer container = PathContainer.parsePath(path);
        for (RequestMappingInfo info : m.getHandlerMethods().keySet()) {
            var methods = info.getMethodsCondition().getMethods();
            boolean readable = methods.isEmpty()
                    || methods.contains(org.springframework.web.bind.annotation.RequestMethod.GET);
            if (!readable) continue;
            PathPatternsRequestCondition patterns = info.getPathPatternsCondition();
            if (patterns == null) continue;
            for (PathPattern pattern : patterns.getPatterns()) {
                if (pattern.matches(container)) return true;
            }
        }
        return false;
    }
}
