package org.example.shop1.model.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.shop1.model.entity.ActivityLog;
import org.example.shop1.model.entity.Category;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.entity.RfSpec;
import org.example.shop1.model.reposritory.CategoryRepository;
import org.example.shop1.model.reposritory.ProductRepository;
import org.example.shop1.model.service.util.ProductUrlUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * دادهٔ فنیِ رادیویی روی محصول و دو مسیرِ قراردادِ نسخهٔ ۲: {@code rf/suggest} و
 * {@code rf/equipment}. قرارداد: {@code docs/dadehlink-api-contract.md}.
 * <p>
 * 🔴 <b>قاعدهٔ انتخابِ کالا فقط همین‌جاست</b> تا اپِ اندرویدی و سایت یک جواب بدهند.
 * کلاینت‌ها فقط نشان می‌دهند؛ محاسبهٔ لینک همچنان با {@code rf.js} در کلاینت است.
 */
@Service
public class RfSpecService {

    static final double FREE_GAIN_TOLERANCE_DB = 0.5;
    static final double BAND_60_MHZ = 57000;
    static final String NOTICE_60 = "برای باندِ ۶۰ گیگاهرتز، آب‌وهوا را روشن کنید تا اثرِ باران در حاشیه بیاید.";
    private static final Pattern FAMILY = Pattern.compile("^[A-Z0-9_]{2,40}(;[A-Z0-9_]{2,40})*$");

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final ActivityLogService activityLog;
    private final ObjectMapper json;
    private final Map<String, String> families;

    @Value("${app.rf.target-margin-db:10}")
    double targetMarginDb;

    public RfSpecService(ProductRepository productRepo, CategoryRepository categoryRepo,
                         ActivityLogService activityLog, ObjectMapper json) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.activityLog = activityLog;
        this.json = json;
        this.families = loadFamilies(json);
    }

    private static Map<String, String> loadFamilies(ObjectMapper json) {
        try (InputStream in = new ClassPathResource("rf/families.json").getInputStream()) {
            return json.readValue(in, new TypeReference<LinkedHashMap<String, String>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    // ==========================================================
    // اعتبارسنجی
    // ==========================================================

    /** فهرستِ خطاها به زبانِ ادمین؛ خالی یعنی معتبر. */
    public static List<String> validate(RfSpec rf) {
        List<String> e = new ArrayList<>();
        if (rf == null) return e;
        if (rf.getKind() == null || !RfSpec.KINDS.contains(rf.getKind())) e.add("نوع باید یکی از ANTENNA، RADIO یا RADIO_INTEGRATED باشد");
        String kind = rf.getKind();
        if (rf.getBands() != null) {
            for (int i = 0; i < rf.getBands().size(); i++) {
                RfSpec.Band b = rf.getBands().get(i);
                if (b == null || b.getMinMhz() == null || b.getMaxMhz() == null) { e.add("باندِ " + (i + 1) + ": هر دو سر لازم است"); continue; }
                if (b.getMinMhz() < 100 || b.getMaxMhz() > 100000) e.add("باندِ " + (i + 1) + ": بیرون از ۱۰۰ تا ۱۰۰٬۰۰۰ مگاهرتز");
                if (b.getMinMhz() >= b.getMaxMhz()) e.add("باندِ " + (i + 1) + ": ابتدا باید از انتها کمتر باشد");
            }
        }
        RfSpec.Antenna a = rf.getAntenna();
        if (a != null) {
            if ("RADIO".equals(kind)) e.add("رادیوی کانکتوردار بخشِ آنتن ندارد");
            range(e, "بهرهٔ آنتن", a.getGainDbi(), 0, 60);
            range(e, "قطرِ دیش", a.getDiameterCm(), 1, 500);
            range(e, "پهنای پرتو", a.getBeamwidthDeg(), 0.1, 360);
            if (a.getType() != null && !RfSpec.ANTENNA_TYPES.contains(a.getType())) e.add("نوعِ آنتن نامعتبر است");
            if (a.getPolarization() != null && !RfSpec.POLARIZATIONS.contains(a.getPolarization())) e.add("قطبش باید SINGLE یا DUAL باشد");
        }
        RfSpec.Radio r = rf.getRadio();
        if (r != null) {
            if ("ANTENNA".equals(kind)) e.add("آنتن بخشِ رادیو ندارد");
            range(e, "توانِ ارسال", r.getTxMaxDbm(), -30, 50);
            range(e, "حساسیت در کمترین نرخ", r.getSensLowDbm(), -130, -20);
            range(e, "حساسیت در بیشترین نرخ", r.getSensHighDbm(), -130, -20);
            if (r.getCompatFamily() != null && !FAMILY.matcher(r.getCompatFamily()).matches()) e.add("کدِ خانواده فقط حروفِ بزرگِ لاتین، عدد و _ است");
            len(e, "شرطِ توان", r.getTxCond(), 200);
            len(e, "شرطِ حساسیتِ کم", r.getSensLowCond(), 200);
            len(e, "شرطِ حساسیتِ زیاد", r.getSensHighCond(), 200);
        }
        if (rf.getRates() != null) {
            if (!rf.getRates().isEmpty() && "ANTENNA".equals(kind)) e.add("آنتن جدولِ نرخ ندارد");
            for (int i = 0; i < rf.getRates().size(); i++) {
                RfSpec.Rate x = rf.getRates().get(i);
                String p = "نرخِ " + (i + 1) + ": ";
                if (x == null) { e.add(p + "خالی"); continue; }
                if (x.getMinMhz() != null && x.getMaxMhz() != null && x.getMinMhz() >= x.getMaxMhz()) e.add(p + "باند نامعتبر");
                range(e, p + "پهنای کانال", x.getChannelMhz(), 0.1, 2200);
                range(e, p + "توان", x.getTxDbm(), -30, 50);
                range(e, p + "حساسیت", x.getSensDbm(), -130, -20);
                range(e, p + "سرعت", x.getRateMbps(), 0, 100000);
                len(e, p + "برچسب", x.getRateLabel(), 60);
            }
        }
        if (rf.getSource() != null) {
            String u = rf.getSource().getUrl();
            if (u != null && !u.isBlank() && (!u.matches("^https?://\\S+$") || u.length() > 600)) e.add("نشانیِ منبع باید http یا https باشد");
            len(e, "تاریخِ بررسی", rf.getSource().getChecked(), 20);
        }
        return e;
    }

    private static void range(List<String> e, String name, Double v, double lo, double hi) {
        if (v != null && (v.isNaN() || v < lo || v > hi)) e.add(name + " باید بینِ " + lo + " و " + hi + " باشد");
    }

    private static void len(List<String> e, String name, String v, int max) {
        if (v != null && v.length() > max) e.add(name + " حداکثر " + max + " نویسه");
    }

    // ==========================================================
    // ویرایشِ دستی از مودالِ محصول
    // ==========================================================

    public Product updateManual(String productId, RfSpec rf) {
        Product p = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("محصول پیدا نشد"));
        RfSpec clean = normalize(rf);
        List<String> errors = validate(clean);
        if (!errors.isEmpty()) throw new IllegalArgumentException(String.join("؛ ", errors));
        if (sameAs(p.getRf(), clean)) return p;

        String before = summary(p.getRf());
        p.setRf(clean);
        p.setRfUpdatedAt(Instant.now());
        Product saved = productRepo.save(p);
        activityLog.recordProduct(ActivityLog.Action.PRODUCT_UPDATE, ActivityLog.Source.MANUAL,
                p.getId(), p.getName(), "rf", before, summary(clean));
        return saved;
    }

    /** خانه‌های رشته‌ایِ خالی نال، فهرست‌های نال خالی؛ بخشِ کاملاً خالی حذف. */
    public static RfSpec normalize(RfSpec rf) {
        if (rf == null) return null;
        if (rf.getBands() == null) rf.setBands(new ArrayList<>());
        if (rf.getRates() == null) rf.setRates(new ArrayList<>());
        RfSpec.Antenna a = rf.getAntenna();
        if (a != null) {
            a.setType(blank(a.getType()));
            a.setPolarization(blank(a.getPolarization()));
            if (a.getGainDbi() == null && a.getType() == null && a.getDiameterCm() == null
                    && a.getBeamwidthDeg() == null && a.getPolarization() == null) rf.setAntenna(null);
        }
        RfSpec.Radio r = rf.getRadio();
        if (r != null) {
            r.setTxCond(blank(r.getTxCond()));
            r.setSensLowCond(blank(r.getSensLowCond()));
            r.setSensHighCond(blank(r.getSensHighCond()));
            r.setCompatFamily(blank(r.getCompatFamily()));
            if (r.getTxMaxDbm() == null && r.getSensLowDbm() == null && r.getSensHighDbm() == null
                    && r.getCompatFamily() == null && r.getTxCond() == null) rf.setRadio(null);
        }
        if (rf.getSource() != null) {
            rf.getSource().setUrl(blank(rf.getSource().getUrl()));
            rf.getSource().setChecked(blank(rf.getSource().getChecked()));
            if (rf.getSource().getUrl() == null && rf.getSource().getChecked() == null) rf.setSource(null);
        }
        for (RfSpec.Rate x : rf.getRates()) if (x != null) x.setRateLabel(blank(x.getRateLabel()));
        return rf;
    }

    /** مقایسهٔ ساختاری با JSON؛ ترتیبِ خانه‌ها ثابت است (ترتیبِ تعریف). */
    public boolean sameAs(RfSpec a, RfSpec b) {
        try {
            return Objects.equals(a == null ? null : json.writeValueAsString(a),
                    b == null ? null : json.writeValueAsString(b));
        } catch (Exception e) {
            return false;
        }
    }

    public static String summary(RfSpec rf) {
        if (rf == null) return "—";
        StringBuilder sb = new StringBuilder(String.valueOf(rf.getKind()));
        if (rf.getBands() != null) for (RfSpec.Band b : rf.getBands()) sb.append(" · ").append(b.getMinMhz()).append('-').append(b.getMaxMhz());
        if (rf.getAntenna() != null && rf.getAntenna().getGainDbi() != null) sb.append(" · ").append(rf.getAntenna().getGainDbi()).append(" dBi");
        if (rf.getRadio() != null) {
            if (rf.getRadio().getTxMaxDbm() != null) sb.append(" · tx ").append(rf.getRadio().getTxMaxDbm());
            if (rf.getRadio().getSensLowDbm() != null) sb.append(" · sens ").append(rf.getRadio().getSensLowDbm());
            if (rf.getRadio().getCompatFamily() != null) sb.append(" · ").append(rf.getRadio().getCompatFamily());
        }
        if (rf.getRates() != null && !rf.getRates().isEmpty()) sb.append(" · ").append(rf.getRates().size()).append(" نرخ");
        return sb.toString();
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    // ==========================================================
    // کارتِ محصول — شکلِ مشترکِ هر دو مسیر
    // ==========================================================

    public Map<String, Object> card(Product p, String baseUrl, Map<String, Category> categories) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("brand", brandOf(p, categories));
        m.put("url", baseUrl + ProductUrlUtil.hybridPathEncoded(p));
        String img = p.getImages() == null || p.getImages().isEmpty() ? null : p.getImages().get(0);
        m.put("image", img == null || img.isBlank() ? null : (img.startsWith("http") ? img : baseUrl + (img.startsWith("/") ? "" : "/") + img));
        BigDecimal price = p.getOnlinePrice() != null ? p.getOnlinePrice() : p.getPrice();
        m.put("priceToman", price == null || price.signum() <= 0 ? null : price.setScale(0, java.math.RoundingMode.HALF_UP).longValue());
        m.put("inStock", inStock(p));
        m.put("rf", p.getRf());
        return m;
    }

    static boolean inStock(Product p) {
        return p.getStock() != null && p.getStock() > 0;
    }

    /** برند از نزدیک‌ترین دسته‌ای که brandName دارد — همان قاعدهٔ اسکیمای صفحهٔ محصول. */
    static String brandOf(Product p, Map<String, Category> cats) {
        Category own = p.getCategoryId() == null ? null : cats.get(p.getCategoryId());
        if (own == null) return null;
        if (blank(own.getBrandName()) != null) return own.getBrandName().trim();
        List<String> anc = own.getAncestors();
        if (anc == null) return null;
        for (int i = anc.size() - 1; i >= 0; i--) {
            Category c = cats.get(anc.get(i));
            if (c != null && blank(c.getBrandName()) != null) return c.getBrandName().trim();
        }
        return null;
    }

    public Map<String, Category> categoryMap() {
        Map<String, Category> m = new HashMap<>();
        for (Category c : categoryRepo.findAll()) m.put(c.getId(), c);
        return m;
    }

    /** محصولاتِ دارای rf، بی «توقفِ تولید». */
    public List<Product> catalog() {
        return productRepo.findAll().stream()
                .filter(p -> p.getRf() != null && p.getRf().getKind() != null)
                .filter(p -> !p.isProductionStopped())
                .toList();
    }

    // ==========================================================
    // rf/suggest
    // ==========================================================

    public record SuggestQuery(double fMhz, double gainA, double gainB, double marginDb,
                               double txDbm, double sensDbm, boolean weather) {}

    public record Suggestion(double targetMarginDb, double minGainA, double minGainB,
                             List<Product> antennasA, List<Product> antennasB, List<Product> radios, String notice) {}

    /** قاعدهٔ قرارداد، بی هیچ وابستگی به دیتابیس — همین تست می‌شود. */
    public static Suggestion suggest(List<Product> catalog, SuggestQuery q, double target) {
        double deficitHalf = Math.max(0, target - q.marginDb()) / 2;
        double minA = round1(q.gainA() + deficitHalf);
        double minB = round1(q.gainB() + deficitHalf);
        if (q.fMhz() >= BAND_60_MHZ && !q.weather()) {
            return new Suggestion(target, minA, minB, List.of(), List.of(), List.of(), NOTICE_60);
        }
        List<Product> antA = antennas(catalog, q.fMhz(), minA);
        List<Product> antB = minA == minB ? antA : antennas(catalog, q.fMhz(), minB);
        double needIntegrated = Math.max(minA, minB);
        List<Product> radios = catalog.stream()
                .filter(p -> "RADIO".equals(p.getRf().getKind()) || "RADIO_INTEGRATED".equals(p.getRf().getKind()))
                .filter(p -> p.getRf().coversMhz(q.fMhz()))
                .filter(p -> {
                    RfSpec.Radio r = p.getRf().getRadio();
                    return r != null && r.getTxMaxDbm() != null && r.getSensLowDbm() != null
                            && r.getTxMaxDbm() >= q.txDbm() && r.getSensLowDbm() <= q.sensDbm();
                })
                .filter(p -> !"RADIO_INTEGRATED".equals(p.getRf().getKind())
                        || (p.getRf().getAntenna() != null && p.getRf().getAntenna().getGainDbi() != null
                            && p.getRf().getAntenna().getGainDbi() >= needIntegrated - FREE_GAIN_TOLERANCE_DB))
                .sorted(Comparator.comparing((Product p) -> !inStock(p)).thenComparing(RfSpecService::priceKey)
                        .thenComparing(p -> String.valueOf(p.getName())))
                .limit(4).toList();
        return new Suggestion(target, minA, minB, antA, antB, radios, null);
    }

    private static List<Product> antennas(List<Product> catalog, double fMhz, double minGain) {
        return catalog.stream()
                .filter(p -> "ANTENNA".equals(p.getRf().getKind()))
                .filter(p -> p.getRf().coversMhz(fMhz))
                .filter(p -> p.getRf().getAntenna() != null && p.getRf().getAntenna().getGainDbi() != null
                        && p.getRf().getAntenna().getGainDbi() >= minGain - FREE_GAIN_TOLERANCE_DB)
                .sorted(Comparator.comparing((Product p) -> !inStock(p))
                        .thenComparing(p -> p.getRf().getAntenna().getGainDbi())
                        .thenComparing(RfSpecService::priceKey))
                .limit(3).toList();
    }

    /** بی‌قیمت آخر. */
    private static BigDecimal priceKey(Product p) {
        BigDecimal v = p.getOnlinePrice() != null ? p.getOnlinePrice() : p.getPrice();
        return v == null || v.signum() <= 0 ? new BigDecimal("1e18") : v;
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    // ==========================================================
    // rf/equipment
    // ==========================================================

    public Map<String, Object> equipment(String baseUrl) {
        List<Product> all = catalog();
        Map<String, Category> cats = categoryMap();
        List<Map<String, Object>> radios = new ArrayList<>(), antennas = new ArrayList<>();
        Instant version = Instant.EPOCH;
        Map<String, String> fam = new LinkedHashMap<>();
        for (Product p : all.stream().sorted(Comparator.comparing(p -> String.valueOf(p.getName()))).toList()) {
            if (p.getRfUpdatedAt() != null && p.getRfUpdatedAt().isAfter(version)) version = p.getRfUpdatedAt();
            if ("ANTENNA".equals(p.getRf().getKind())) antennas.add(card(p, baseUrl, cats));
            else radios.add(card(p, baseUrl, cats));
            RfSpec.Radio r = p.getRf().getRadio();
            if (r != null && r.getCompatFamily() != null) {
                for (String code : r.getCompatFamily().split(";")) fam.put(code, families.getOrDefault(code, code));
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("version", version.toString());
        out.put("radios", radios);
        out.put("antennas", antennas);
        out.put("families", fam);
        return out;
    }

    public double targetMarginDb() {
        return targetMarginDb;
    }
}
