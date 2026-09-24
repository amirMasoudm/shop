package org.example.shop1.model.service;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.example.shop1.model.entity.Product;
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
 * <b>سه قاعده</b>، به همین ترتیب امتحان می‌شوند و اولین مقصدِ معتبر برنده است:
 * <ol>
 *   <li><b>اسلشِ پایانی</b> — تطبیقِ اسلشِ پایانی در اسپرینگ‌بوت ۳ پیش‌فرض خاموش شد،
 *       ولی سایتِ وردپرسی همه‌جا اسلش داشت. اگر نسخهٔ بی‌اسلش هندلر دارد، همان.</li>
 *   <li><b>مسیرِ تک‌بخشی</b> — {@code /{x}} که {@code x} اسلاگِ مقاله‌ای است.</li>
 *   <li><b>{@code /product/{x}}</b> — اگر محصولی با آن اسلاگ باشد.</li>
 * </ol>
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

    private final ArticleService articleService;
    private final ProductRepository productRepo;
    private final LegacyRedirectService legacyRedirects;
    /** با ObjectProvider تا وابستگیِ حلقوی با خودِ مسیریابی درست نشود. */
    private final ObjectProvider<RequestMappingHandlerMapping> mappings;

    public LegacyPathFallback(ArticleService articleService, ProductRepository productRepo,
                              LegacyRedirectService legacyRedirects,
                              ObjectProvider<RequestMappingHandlerMapping> mappings) {
        this.articleService = articleService;
        this.productRepo = productRepo;
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
            String blogPath = "/blog/" + segments[0];
            if (articleService.getPublishedBySlugOrId(segments[0]).isPresent()) {
                log.info("فالبکِ ۴۰۴ [اسلاگِ مقاله]: {} → {}", path, blogPath);
                return blogPath;
            }
            // اسلاگ عوض شده: مقصدِ نهاییِ زنجیره، نه خودِ /blog/{x} که ۳۰۱ می‌دهد
            String chained = followRedirects(blogPath);
            // 🔴 گاردِ حلقه فقط همین‌جا لازم است، و عمداً سراسری نیست. سه قاعدهٔ دیگر
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
                log.info("فالبکِ ۴۰۴ [اسلاگِ مقالهٔ تغییریافته]: {} → {}", path, chained);
                return chained;
            }
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
        return null;
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
