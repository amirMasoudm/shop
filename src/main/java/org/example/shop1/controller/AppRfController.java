package org.example.shop1.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.shop1.exeption.AppApiException;
import org.example.shop1.model.entity.Category;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.service.RfSpecService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * قراردادِ نسخهٔ ۲ — {@code rf/suggest} و {@code rf/equipment}. عمومی و بی‌توکن؛ در زنجیرهٔ
 * امنیتیِ {@code /api/app/v1/**} (همان CORSِ اپ). پاسخ‌ها ده دقیقه کش‌شدنی‌اند.
 */
@RestController
@RequestMapping("/api/app/v1/rf")
public class AppRfController {

    private static final CacheControl TEN_MINUTES = CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic();

    private final RfSpecService rf;

    public AppRfController(RfSpecService rf) {
        this.rf = rf;
    }

    @GetMapping("/suggest")
    public ResponseEntity<?> suggest(@RequestParam Map<String, String> q, HttpServletRequest request) {
        RfSpecService.SuggestQuery query = new RfSpecService.SuggestQuery(
                num(q, "fMhz", 2000, 80000), num(q, "gainA", 0, 80), num(q, "gainB", 0, 80),
                num(q, "marginDb", -400, 400), num(q, "txDbm", -30, 50), num(q, "sensDbm", -130, -20),
                weather(q));
        RfSpecService.Suggestion s = RfSpecService.suggest(rf.catalog(), query, rf.targetMarginDb());
        String base = baseUrl(request);
        Map<String, Category> cats = rf.categoryMap();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("targetMarginDb", s.targetMarginDb());
        out.put("minGainA", s.minGainA());
        out.put("minGainB", s.minGainB());
        out.put("antennasA", cards(s.antennasA(), base, cats));
        out.put("antennasB", cards(s.antennasB(), base, cats));
        out.put("radios", cards(s.radios(), base, cats));
        out.put("notice", s.notice());
        return ResponseEntity.ok().cacheControl(TEN_MINUTES).body(out);
    }

    @GetMapping("/equipment")
    public ResponseEntity<?> equipment(HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(TEN_MINUTES).body(rf.equipment(baseUrl(request)));
    }

    @ExceptionHandler(AppApiException.class)
    public ResponseEntity<Map<String, Object>> onAppError(AppApiException e) {
        return ResponseEntity.status(e.getStatus()).body(e.getBody());
    }

    private List<Map<String, Object>> cards(List<Product> ps, String base, Map<String, Category> cats) {
        return ps.stream().map(p -> rf.card(p, base, cats)).toList();
    }

    /** عدد در بازه، وگرنه ۴۰۰ با نامِ همان پارامتر — مثلِ بقیهٔ قرارداد. */
    static double num(Map<String, String> q, String field, double lo, double hi) {
        String v = q.get(field);
        if (v == null || v.isBlank()) throw AppApiException.invalid(field);
        try {
            double d = Double.parseDouble(v.trim());
            if (Double.isNaN(d) || Double.isInfinite(d) || d < lo || d > hi) throw AppApiException.invalid(field);
            return d;
        } catch (NumberFormatException e) {
            throw AppApiException.invalid(field);
        }
    }

    static boolean weather(Map<String, String> q) {
        String w = q.getOrDefault("weather", "0");
        if ("1".equals(w)) return true;
        if ("0".equals(w)) return false;
        throw AppApiException.invalid("weather");
    }

    /** همان سازوکارِ canonicalِ صفحه‌ها (پشتِ nginx با forward-headers). */
    private static String baseUrl(HttpServletRequest r) {
        int port = r.getServerPort();
        return r.getScheme() + "://" + r.getServerName() + (port == 80 || port == 443 ? "" : ":" + port);
    }
}
