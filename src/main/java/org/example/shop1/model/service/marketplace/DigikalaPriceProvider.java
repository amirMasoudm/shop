package org.example.shop1.model.service.marketplace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * قیمتِ دیجی‌کالا از APIِ جست‌وجویِ عمومی‌اش.
 * <p>
 * <b>نکتهٔ CDN:</b> اولین درخواست {@code 307} می‌دهد و کوکیِ {@code digicdn_cookie}
 * ست می‌کند و به همان آدرس ریدایرکت می‌کند؛ درخواستِ دوم با آن کوکی ۲۰۰ می‌دهد.
 * این رفتارِ استانداردِ HTTP است (نگه‌داشتنِ کوکی + دنبال‌کردنِ ریدایرکت) و با
 * {@link CookieManager} خودکار انجام می‌شود — چیزی دور زده نمی‌شود.
 * <p>
 * <b>🔴 واحد:</b> دیجی‌کالا قیمت را به <b>ریال</b> می‌دهد. نمونهٔ تأییدشده:
 * {@code hAP ax³ = 324,700,000} ریال = ۳۲٬۴۷۰٬۰۰۰ تومان. بدونِ تقسیم بر ۱۰،
 * کفِ قیمت ده برابر ثبت می‌شد و کارشناس فکر می‌کرد خیلی ارزان‌تر از بازاریم.
 */
@Component
public class DigikalaPriceProvider implements MarketplacePriceProvider {

    private static final Logger log = LoggerFactory.getLogger(DigikalaPriceProvider.class);

    private static final String SEARCH_API = "https://api.digikala.com/v1/search/?q=";
    private static final String PRODUCT_API = "https://api.digikala.com/v2/product/%s/";
    private static final String PRODUCT_PAGE = "https://www.digikala.com/product/dkp-%s/";
    private static final String SEARCH_PAGE = "https://www.digikala.com/search/?q=";

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/120.0.0.0 Safari/537.36";

    /** ریال → تومان. */
    private static final BigDecimal RIAL_TO_TOMAN = BigDecimal.TEN;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http;

    public DigikalaPriceProvider() {
        CookieManager cookies = new CookieManager();
        cookies.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .cookieHandler(cookies)
                .build();
    }

    @Override
    public String marketKey() { return "digikala"; }

    @Override
    public boolean supportsAutomaticFetch() { return true; }

    @Override
    public String searchPageUrl(String query) {
        return SEARCH_PAGE + enc(query);
    }

    @Override
    public List<Candidate> searchCandidates(String query, int limit) {
        List<Candidate> out = new ArrayList<>();
        if (query == null || query.isBlank()) return out;

        try {
            JsonNode root = getJson(SEARCH_API + enc(query));
            if (root == null) return out;

            JsonNode products = root.path("data").path("products");
            for (JsonNode p : products) {
                if (out.size() >= limit) break;
                String dkp = p.path("id").asText(null);
                if (dkp == null) continue;

                BigDecimal toman = tomanOf(p.path("default_variant").path("price").path("selling_price"));
                out.add(new Candidate(
                        dkp,
                        p.path("title_fa").asText(""),
                        toman,
                        String.format(PRODUCT_PAGE, dkp)));
            }
        } catch (Exception e) {
            log.warn("جست‌وجویِ دیجی‌کالا ناموفق ({}): {}", query, e.toString());
        }
        return out;
    }

    @Override
    public BigDecimal fetchPriceToman(String dkp) {
        if (dkp == null || dkp.isBlank()) return null;
        try {
            JsonNode root = getJson(String.format(PRODUCT_API, enc(dkp)));
            if (root == null) return null;

            JsonNode product = root.path("data").path("product");
            // مسیرِ اصلی: واریانتِ پیش‌فرض
            BigDecimal price = tomanOf(product.path("default_variant").path("price").path("selling_price"));
            if (price != null) return price;

            // اگر واریانتِ پیش‌فرض قیمت نداشت (ناموجود)، کمترینِ واریانت‌ها
            BigDecimal min = null;
            for (JsonNode v : product.path("variants")) {
                BigDecimal c = tomanOf(v.path("price").path("selling_price"));
                if (c != null && (min == null || c.compareTo(min) < 0)) min = c;
            }
            return min;
        } catch (Exception e) {
            log.warn("واکشیِ قیمتِ دیجی‌کالا برایِ DKP {} ناموفق: {}", dkp, e.toString());
            return null;
        }
    }

    /** ریال → تومان؛ صفر/نامعتبر یعنی «قیمت ندارد» (ناموجود). */
    private BigDecimal tomanOf(JsonNode priceNode) {
        if (priceNode == null || !priceNode.isNumber()) return null;
        long rial = priceNode.asLong();
        if (rial <= 0) return null;
        return BigDecimal.valueOf(rial).divide(RIAL_TO_TOMAN, 0, java.math.RoundingMode.HALF_UP);
    }

    private JsonNode getJson(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "fa-IR,fa;q=0.9,en;q=0.8")
                .timeout(Duration.ofSeconds(25))
                .GET().build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() != 200) {
            log.warn("دیجی‌کالا کدِ {} داد برایِ {}", res.statusCode(), url);
            return null;
        }
        return mapper.readTree(res.body());
    }

    private String enc(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
