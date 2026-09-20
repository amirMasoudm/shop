package org.example.shop1.model.service.marketplace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * قیمتِ مرجعِ دلاریِ میکروتیک، مستقیم از سایتِ خودِ میکروتیک.
 * <p>
 * <b>چرا این منبع:</b> {@code robots.txt}ِ میکروتیک صریحاً همه‌چیز را باز گذاشته
 * ({@code Disallow:} خالی) و خودش {@code sitemap.xml} را معرفی می‌کند؛ پس این
 * خزشِ مجاز است، نه دور زدنِ چیزی. صفحهٔ هر محصول هم «Product code» و
 * «Suggested price» را <b>سمتِ سرور</b> رندر می‌کند، یعنی با یک GET ساده می‌آید.
 * <p>
 * <b>🔴 این عدد MSRP است، نه قیمتِ خریدِ نماینده.</b> تنها عددی است که میکروتیک
 * عمومی منتشر می‌کند و مالک با همین موافقت کرد؛ ستونِ میزِ کار هم به همین دلیل
 * «مرجع $» نام گرفت تا با قیمتِ خرید اشتباه نشود.
 * <p>
 * <b>چرا صفحهٔ محصول و نه فهرست:</b> فهرستِ {@code /products} فقط ۲۵ تا را
 * سمتِ سرور می‌دهد و بقیه با Livewire می‌آیند؛ ضمناً فهرست <b>کدِ قطعه ندارد</b>
 * و تطبیق با نامِ بازاریابی شکننده می‌شد. صفحهٔ محصول کدِ قطعه دارد و کدِ قطعه
 * داخلِ نامِ محصولاتِ خودِ ماست، پس تطبیق دقیق می‌شود.
 */
@Service
public class MikrotikCatalogService {

    private static final Logger log = LoggerFactory.getLogger(MikrotikCatalogService.class);

    private static final String SITEMAP = "https://mikrotik.com/sitemap.xml";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/120.0.0.0 Safari/537.36";

    /** تازه‌تر از این باشد، دوباره خزیده نمی‌شود. */
    private static final Duration CACHE_TTL = Duration.ofHours(20);

    /** هم‌زمانیِ عمدی کم است: خزشِ مجاز دلیلِ بی‌ملاحظگی نیست. */
    private static final int WORKERS = 4;

    private static final Pattern LOC = Pattern.compile("<loc>\\s*(https://mikrotik\\.com/product/[^<\\s]+)\\s*</loc>");
    private static final Pattern CODE = Pattern.compile(
            "Product code\\s*</span>\\s*<span[^>]*>\\s*([^<]+?)\\s*</span>", Pattern.DOTALL);
    private static final Pattern PRICE = Pattern.compile(
            "Suggested price\\s*</span>\\s*<span[^>]*>\\s*\\$\\s*([0-9][0-9.,]*)\\s*</span>", Pattern.DOTALL);
    private static final Pattern TITLE = Pattern.compile("<h1[^>]*>\\s*([^<]+?)\\s*</h1>", Pattern.DOTALL);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** کدِ نرمال‌شدهٔ قطعه → برداشتِ آن محصول. */
    private final Map<String, Entry> catalog = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger done = new AtomicInteger();
    private final AtomicInteger total = new AtomicInteger();
    private volatile Instant updatedAt;
    private volatile String lastError;

    /** یک محصولِ میکروتیک، آن‌طور که سایتِ خودشان می‌گوید. */
    public record Entry(String code, String name, BigDecimal usd, String url) {}

    /** وضعیتِ خزش برایِ نوارِ پیشرفتِ پنل. */
    public Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("running", running.get());
        m.put("done", done.get());
        m.put("total", total.get());
        m.put("count", catalog.size());
        m.put("updatedAt", updatedAt == null ? null : updatedAt.toString());
        m.put("fresh", isFresh());
        m.put("error", lastError);
        return m;
    }

    public boolean isFresh() {
        return updatedAt != null && !catalog.isEmpty()
                && Duration.between(updatedAt, Instant.now()).compareTo(CACHE_TTL) < 0;
    }

    public Map<String, Entry> snapshot() {
        return Map.copyOf(catalog);
    }

    /**
     * خزشِ کاتالوگ را شروع می‌کند و بی‌درنگ برمی‌گردد.
     * <p>
     * ⚠️ اگر کش تازه است و {@code force} نیست، اصلاً درخواستی نمی‌رود — دکمهٔ
     * پنل را می‌شود چند بار زد بدون اینکه سایتِ میکروتیک را بکوبد.
     */
    public synchronized Map<String, Object> startRefresh(boolean force) {
        if (running.get()) return status();
        if (!force && isFresh()) return status();

        running.set(true);
        done.set(0);
        total.set(0);
        lastError = null;

        // نخِ جدا، چون خزش دقیقه‌ای طول می‌کشد و نباید درخواستِ HTTP را معلق نگه دارد.
        Thread t = new Thread(this::crawl, "mikrotik-catalog");
        t.setDaemon(true);
        t.start();
        return status();
    }

    private void crawl() {
        ExecutorService pool = Executors.newFixedThreadPool(WORKERS);
        try {
            String xml = get(SITEMAP);
            if (xml == null) throw new IllegalStateException("sitemap در دسترس نیست");

            List<String> urls = new ArrayList<>();
            Matcher m = LOC.matcher(xml);
            while (m.find()) urls.add(m.group(1));
            total.set(urls.size());
            log.info("خزشِ کاتالوگِ میکروتیک: {} صفحهٔ محصول", urls.size());

            Map<String, Entry> fresh = new ConcurrentHashMap<>();
            for (String url : urls) {
                pool.submit(() -> {
                    try {
                        Entry e = fetchProduct(url);
                        if (e != null) fresh.put(normalizeCode(e.code()), e);
                    } catch (Exception ex) {
                        log.debug("صفحهٔ {} خوانده نشد: {}", url, ex.toString());
                    } finally {
                        done.incrementAndGet();
                    }
                });
            }
            pool.shutdown();
            if (!pool.awaitTermination(10, TimeUnit.MINUTES)) pool.shutdownNow();

            // ⚠️ فقط وقتی چیزی به دست آمد جایگزین کن؛ خزشِ ناموفق نباید
            // کاتالوگِ سالمِ قبلی را پاک کند.
            if (!fresh.isEmpty()) {
                catalog.clear();
                catalog.putAll(fresh);
                updatedAt = Instant.now();
            }
            log.info("کاتالوگِ میکروتیک: {} محصولِ قیمت‌دار", catalog.size());
        } catch (Exception e) {
            lastError = e.toString();
            log.warn("خزشِ کاتالوگِ میکروتیک ناموفق: {}", e.toString());
        } finally {
            pool.shutdownNow();
            running.set(false);
        }
    }

    private Entry fetchProduct(String url) {
        String html = get(url);
        if (html == null) return null;

        Matcher c = CODE.matcher(html);
        Matcher p = PRICE.matcher(html);
        if (!c.find() || !p.find()) return null;

        String code = unescape(c.group(1));
        BigDecimal usd;
        try {
            usd = new BigDecimal(p.group(1).replace(",", ""));
        } catch (Exception e) {
            return null;
        }
        if (code.isEmpty() || usd.signum() <= 0) return null;

        Matcher t = TITLE.matcher(html);
        String name = t.find() ? unescape(t.group(1)) : code;
        return new Entry(code, name, usd, url);
    }

    /**
     * ⚠️ لازم است و با دادهٔ واقعی دیده شد: کدِ {@code RBLHGR&R11e-4G} در HTML به شکلِ
     * {@code RBLHGR&amp;R11e-4G} می‌آید. بدونِ بازگشایی، نرمال‌سازی آن را به
     * {@code rblhgrampr11e4g} تبدیل می‌کرد و هیچ‌وقت با نامِ محصولِ ما جور نمی‌شد.
     */
    private static String unescape(String s) {
        return s.trim()
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .trim();
    }

    private String get(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .timeout(Duration.ofSeconds(25))
                    .GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() != 200) return null;
            return res.body();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * کدِ قطعه را به شکلِ قابلِ‌مقایسه درمی‌آورد: فقط حرف و رقم، همه کوچک.
     * <p>
     * ⚠️ لازم است چون نامِ محصولاتِ ما کد را با فاصله/خط‌تیره/زیرخطِ دلخواه
     * می‌نویسد: «RBcAPGi-5acD2nD» و «RBcAPGi 5acD2nD» باید یکی حساب شوند.
     */
    public static String normalizeCode(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char ch : s.toCharArray()) {
            if (Character.isLetterOrDigit(ch) && ch < 128) sb.append(Character.toLowerCase(ch));
        }
        return sb.toString();
    }
}
