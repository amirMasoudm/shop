package org.example.shop1.model.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.shop1.model.entity.ActivityLog;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.entity.RfSpec;
import org.example.shop1.model.reposritory.ProductRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * واردکردنِ دادهٔ فنیِ چت ب از دو CSV — {@code rf-specs-*.csv} (یک ردیف برای هر محصول) و
 * {@code rf-specs-mcs-*.csv} (یک ردیف برای هر نرخ). ستون‌ها:
 * {@code docs/prompt-data-chat-rf-specs-research.md}.
 * <p>
 * 🔴 <b>فقط {@code rf} نوشته می‌شود.</b> نه نام، نه قیمت، نه متن — حتی وقتی ستونِ {@code name}
 * فایل با نامِ محصول نمی‌خواند. تطبیق فقط با {@code product_id}.
 * <p>
 * دو مرحله، مثلِ ایمپورتِ هلو: پیش‌نمایش (بی‌نوشتن) و اعمال. اعمال دوباره با دیتابیس مقایسه
 * می‌کند، پس اجرای دوباره با همان فایل صفر تغییر می‌دهد. برای هر اجرا فقط یک رکوردِ خلاصه
 * در {@code activity_logs}.
 * <p>
 * عددی که عدد نیست (مثلاً «±2.5») یا «در منبع نیست» خالی می‌ماند، با هشدار در پیش‌نمایش.
 * حدس زده نمی‌شود.
 */
@Service
public class RfSpecImportService {

    static final String NOT_IN_SOURCE = "در منبع نیست";
    private static final Duration PREVIEW_TTL = Duration.ofMinutes(30);

    private final ProductRepository productRepo;
    private final RfSpecService rfService;
    private final ActivityLogService activityLog;
    private final ObjectMapper json;
    private final Map<String, Preview> previews = new ConcurrentHashMap<>();

    public RfSpecImportService(ProductRepository productRepo, RfSpecService rfService,
                               ActivityLogService activityLog, ObjectMapper json) {
        this.productRepo = productRepo;
        this.rfService = rfService;
        this.activityLog = activityLog;
        this.json = json;
    }

    public record Change(String field, String from, String to) {}

    public record Item(String productId, String holooCode, String fileName, String productName, String status,
                       List<Change> changes, List<String> errors, List<String> warnings) {}

    public record Preview(String token, List<String> fileNames, int rowsRead, int rateRowsRead,
                          List<Item> items, Map<String, Integer> counts, Instant at,
                          Map<String, RfSpec> specs) {}

    // ==========================================================
    // پیش‌نمایش
    // ==========================================================

    public Preview preview(String productsCsv, String productsName, String ratesCsv, String ratesName) {
        previews.entrySet().removeIf(e -> e.getValue().at().isBefore(Instant.now().minus(PREVIEW_TTL)));

        List<Map<String, String>> rows = Csv.parse(productsCsv);
        List<Map<String, String>> rateRows = ratesCsv == null || ratesCsv.isBlank() ? List.of() : Csv.parse(ratesCsv);
        Map<String, List<Map<String, String>>> ratesBy = new LinkedHashMap<>();
        for (Map<String, String> r : rateRows) ratesBy.computeIfAbsent(trim(r.get("product_id")), k -> new ArrayList<>()).add(r);

        List<Item> items = new ArrayList<>();
        Map<String, RfSpec> specs = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String k : List.of("new", "changed", "same", "invalid", "empty", "unmatched")) counts.put(k, 0);

        for (Map<String, String> row : rows) {
            String id = trim(row.get("product_id"));
            List<String> warnings = new ArrayList<>();
            Optional<Product> found = id == null ? Optional.empty() : productRepo.findById(id);
            if (found.isEmpty()) {
                items.add(new Item(id, trim(row.get("holoo_code")), trim(row.get("name")), null, "unmatched",
                        List.of(), List.of("محصولی با این product_id در دیتابیس نیست"), warnings));
                counts.merge("unmatched", 1, Integer::sum);
                continue;
            }
            Product p = found.get();
            String kind = trim(row.get("kind"));
            if (kind == null) {
                items.add(new Item(id, trim(row.get("holoo_code")), trim(row.get("name")), p.getName(), "empty",
                        List.of(), List.of("ستونِ kind خالی است؛ چیزی وارد نمی‌شود"), warnings));
                counts.merge("empty", 1, Integer::sum);
                continue;
            }
            RfSpec rf = RfSpecService.normalize(build(row, ratesBy.getOrDefault(id, List.of()), warnings));
            List<String> errors = RfSpecService.validate(rf);
            String status;
            if (!errors.isEmpty()) status = "invalid";
            else if (p.getRf() == null) status = "new";
            else if (rfService.sameAs(p.getRf(), rf)) status = "same";
            else status = "changed";
            counts.merge(status, 1, Integer::sum);
            if (errors.isEmpty()) specs.put(id, rf);
            items.add(new Item(id, trim(row.get("holoo_code")), trim(row.get("name")), p.getName(), status,
                    "same".equals(status) ? List.of() : diff(p.getRf(), rf), errors, warnings));
        }
        // نرخی که محصولش در فایلِ اول نیست
        for (String pid : ratesBy.keySet()) {
            if (pid != null && rows.stream().noneMatch(r -> pid.equals(trim(r.get("product_id"))))) {
                items.add(new Item(pid, null, null, null, "unmatched", List.of(),
                        List.of("ردیف‌های نرخ برای محصولی که در فایلِ اول نیست (" + ratesBy.get(pid).size() + " ردیف)"), List.of()));
                counts.merge("unmatched", 1, Integer::sum);
            }
        }

        List<String> names = new ArrayList<>();
        names.add(productsName);
        if (ratesName != null) names.add(ratesName);
        Preview pv = new Preview(UUID.randomUUID().toString(), names, rows.size(), rateRows.size(),
                items, counts, Instant.now(), specs);
        previews.put(pv.token(), pv);
        return pv;
    }

    // ==========================================================
    // اعمال
    // ==========================================================

    public record ApplyResult(int created, int changed, int unchanged, int skipped) {}

    public ApplyResult apply(String token) {
        Preview pv = token == null ? null : previews.remove(token);
        if (pv == null) throw new IllegalArgumentException("پیش‌نمایش پیدا نشد یا منقضی شده؛ دوباره فایل را بدهید");
        int created = 0, changed = 0, unchanged = 0, skipped = 0;
        Instant now = Instant.now();
        for (Map.Entry<String, RfSpec> e : pv.specs().entrySet()) {
            Optional<Product> found = productRepo.findById(e.getKey());
            if (found.isEmpty()) { skipped++; continue; }
            Product p = found.get();
            if (rfService.sameAs(p.getRf(), e.getValue())) { unchanged++; continue; }
            if (p.getRf() == null) created++; else changed++;
            p.setRf(e.getValue());
            p.setRfUpdatedAt(now);
            productRepo.save(p);
        }
        activityLog.record(ActivityLog.Action.RF_SPEC_IMPORT, ActivityLog.Source.BATCH,
                ActivityLogService.ENTITY_PRODUCT, null, String.join("، ", pv.fileNames()), "rfImport",
                "خوانده " + pv.rowsRead() + " محصول و " + pv.rateRowsRead() + " نرخ · ردشده "
                        + (pv.counts().get("invalid") + pv.counts().get("empty") + pv.counts().get("unmatched")),
                "تازه " + created + " · عوض‌شده " + changed + " · بی‌تغییر " + unchanged);
        return new ApplyResult(created, changed, unchanged, skipped
                + pv.counts().get("invalid") + pv.counts().get("empty") + pv.counts().get("unmatched"));
    }

    // ==========================================================
    // ساختنِ rf از ردیف
    // ==========================================================

    static RfSpec build(Map<String, String> row, List<Map<String, String>> rateRows, List<String> warnings) {
        RfSpec rf = new RfSpec();
        String kind = trim(row.get("kind"));
        rf.setKind(kind);
        rf.setBands(bands(row.get("bands_mhz"), "bands_mhz", warnings));

        if ("ANTENNA".equals(kind) || "RADIO_INTEGRATED".equals(kind)) {
            RfSpec.Antenna a = new RfSpec.Antenna();
            a.setGainDbi(num(row, "gain_dbi", warnings));
            a.setType(text(row.get("antenna_type")));
            a.setDiameterCm(num(row, "diameter_cm", warnings));
            a.setBeamwidthDeg(num(row, "beamwidth_deg", warnings));
            a.setPolarization(text(row.get("polarization")));
            rf.setAntenna(a);
        }
        if ("RADIO".equals(kind) || "RADIO_INTEGRATED".equals(kind)) {
            RfSpec.Radio r = new RfSpec.Radio();
            r.setTxMaxDbm(num(row, "tx_max_dbm", warnings));
            r.setTxCond(text(row.get("tx_cond")));
            r.setSensLowDbm(num(row, "sens_low_dbm", warnings));
            r.setSensLowCond(text(row.get("sens_low_cond")));
            r.setSensHighDbm(num(row, "sens_high_dbm", warnings));
            r.setSensHighCond(text(row.get("sens_high_cond")));
            r.setCompatFamily(text(row.get("compat_family")));
            rf.setRadio(r);
        }
        List<RfSpec.Rate> rates = new ArrayList<>();
        for (Map<String, String> rr : rateRows) {
            RfSpec.Rate x = new RfSpec.Rate();
            List<RfSpec.Band> b = bands(rr.get("band_mhz"), "band_mhz (نرخ)", warnings);
            if (b.size() == 1) { x.setMinMhz(b.get(0).getMinMhz()); x.setMaxMhz(b.get(0).getMaxMhz()); }
            x.setChannelMhz(num(rr, "channel_mhz", warnings));
            x.setRateLabel(text(rr.get("rate_label")));
            x.setTxDbm(num(rr, "tx_dbm", warnings));
            x.setSensDbm(num(rr, "sens_dbm", warnings));
            x.setRateMbps(num(rr, "rate_mbps", warnings));
            rates.add(x);
        }
        rf.setRates(rates);
        String url = text(row.get("source_url")), checked = text(row.get("source_checked"));
        if (url != null || checked != null) rf.setSource(new RfSpec.Source(url, checked));
        return rf;
    }

    static List<RfSpec.Band> bands(String raw, String col, List<String> warnings) {
        List<RfSpec.Band> out = new ArrayList<>();
        String v = text(raw);
        if (v == null) return out;
        for (String part : v.split(";")) {
            String s = part.trim();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\d{3,6})\\s*-\\s*(\\d{3,6})$").matcher(s);
            if (m.matches()) out.add(new RfSpec.Band(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))));
            else warn(warnings, col + ": «" + s + "» بازهٔ مگاهرتز نیست؛ خالی ماند");
        }
        return out;
    }

    static Double num(Map<String, String> row, String col, List<String> warnings) {
        String v = text(row.get(col));
        if (v == null) return null;
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            warn(warnings, col + ": «" + v + "» عدد نیست؛ خالی ماند");
            return null;
        }
    }

    private static void warn(List<String> warnings, String w) {
        if (!warnings.contains(w)) warnings.add(w);
    }

    /** «در منبع نیست» همان خالی است. */
    static String text(String v) {
        String t = trim(v);
        return t == null || t.equals(NOT_IN_SOURCE) ? null : t;
    }

    static String trim(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    // ==========================================================
    // مقایسه برای پیش‌نمایش
    // ==========================================================

    List<Change> diff(RfSpec before, RfSpec after) {
        Map<String, String> a = flatten(before), b = flatten(after);
        List<Change> out = new ArrayList<>();
        java.util.Set<String> keys = new java.util.LinkedHashSet<>(b.keySet());
        keys.addAll(a.keySet());
        for (String k : keys) {
            String x = a.get(k), y = b.get(k);
            if (!java.util.Objects.equals(x, y)) out.add(new Change(k, x == null ? "—" : x, y == null ? "—" : y));
        }
        return out;
    }

    private Map<String, String> flatten(RfSpec rf) {
        Map<String, String> m = new LinkedHashMap<>();
        if (rf == null) return m;
        JsonNode n = json.valueToTree(rf);
        m.put("kind", text(n.path("kind").asText(null)));
        List<String> bands = new ArrayList<>();
        n.path("bands").forEach(b -> bands.add(b.path("minMhz").asText() + "-" + b.path("maxMhz").asText()));
        if (!bands.isEmpty()) m.put("bands", String.join("; ", bands));
        for (String part : List.of("antenna", "radio", "source")) {
            Iterator<Map.Entry<String, JsonNode>> it = n.path(part).fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> f = it.next();
                m.put(part + "." + f.getKey(), f.getValue().asText());
            }
        }
        int rates = n.path("rates").size();
        if (rates > 0) m.put("rates", rates + " ردیف · " + Integer.toHexString(n.path("rates").toString().hashCode()));
        m.values().removeIf(java.util.Objects::isNull);
        return m;
    }

    // ==========================================================
    // CSV — RFC 4180: نقل‌قول، ویرگول و خطِ تازه داخلِ خانه
    // ==========================================================

    static final class Csv {
        static List<Map<String, String>> parse(String text) {
            List<List<String>> rows = new ArrayList<>();
            List<String> row = new ArrayList<>();
            StringBuilder cell = new StringBuilder();
            boolean quoted = false;
            String s = text.startsWith("﻿") ? text.substring(1) : text;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (quoted) {
                    if (c == '"') {
                        if (i + 1 < s.length() && s.charAt(i + 1) == '"') { cell.append('"'); i++; }
                        else quoted = false;
                    } else cell.append(c);
                } else if (c == '"') quoted = true;
                else if (c == ',') { row.add(cell.toString()); cell.setLength(0); }
                else if (c == '\n' || c == '\r') {
                    if (c == '\r' && i + 1 < s.length() && s.charAt(i + 1) == '\n') i++;
                    row.add(cell.toString()); cell.setLength(0);
                    if (!(row.size() == 1 && row.get(0).isEmpty())) rows.add(row);
                    row = new ArrayList<>();
                } else cell.append(c);
            }
            if (cell.length() > 0 || !row.isEmpty()) { row.add(cell.toString()); rows.add(row); }
            if (rows.isEmpty()) return List.of();
            List<String> header = rows.get(0).stream().map(String::trim).toList();
            List<Map<String, String>> out = new ArrayList<>();
            for (int r = 1; r < rows.size(); r++) {
                Map<String, String> m = new LinkedHashMap<>();
                for (int c = 0; c < header.size(); c++) m.put(header.get(c), c < rows.get(r).size() ? rows.get(r).get(c) : "");
                out.add(m);
            }
            return out;
        }
    }
}
