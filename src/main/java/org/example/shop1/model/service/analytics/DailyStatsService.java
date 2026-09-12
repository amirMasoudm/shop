package org.example.shop1.model.service.analytics;

import org.bson.Document;
import org.example.shop1.config.AnalyticsProperties;
import org.example.shop1.model.entity.DailyStats;
import org.example.shop1.model.enums.EventType;
import org.example.shop1.model.reposritory.DailyStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * جمع‌بندیِ روزانه — داشبورد فقط از خروجیِ همین سرویس می‌خواند.
 * <p>
 * ⚠️ <b>بدونِ اتصال (join) نوشته شده.</b> فازِ ۱ فیلدِ {@code channel} را روی
 * <b>هر</b> رویداد نشانده، پس {@code byChannel} یک {@code group by} ساده است. اگر
 * روزی کسی دید مجبور است برای پیداکردنِ کانالِ یک رویداد به {@code SESSION_START}
 * مراجعه کند، یعنی آن تکرار از بین رفته — آن‌وقت باید فازِ ۱ درست شود، نه اینکه
 * اینجا اتصالِ شبانه اضافه شود.
 */
@Service
public class DailyStatsService {

    private static final Logger log = LoggerFactory.getLogger(DailyStatsService.class);

    private static final int TOP_PRODUCTS = 20;
    private static final int TOP_SEARCHES = 30;

    private final MongoTemplate mongo;
    private final DailyStatsRepository repo;
    private final AnalyticsProperties props;

    public DailyStatsService(MongoTemplate mongo, DailyStatsRepository repo, AnalyticsProperties props) {
        this.mongo = mongo;
        this.repo = repo;
        this.props = props;
    }

    public ZoneId zone() {
        String z = props.getZone();
        if (z == null || z.isBlank()) return ZoneId.systemDefault();
        try {
            return ZoneId.of(z);
        } catch (Exception e) {
            return ZoneId.systemDefault();
        }
    }

    /**
     * جمع‌بندیِ یک روز.
     * <p>
     * <b>قابلِ اجرای دوباره است:</b> شناسهٔ سند خودِ تاریخ است و {@code save} بازنویسی
     * می‌کند، پس اجرای دوم همان عددها را می‌دهد نه سندِ دوم و نه جمعِ دوباره.
     * روزِ بی‌داده هم سندِ صفر می‌گیرد — نبودِ سند در نمودار «حفره» می‌شود نه «صفر».
     */
    public DailyStats rollup(LocalDate day) {
        Instant from = day.atStartOfDay(zone()).toInstant();
        Instant to = day.plusDays(1).atStartOfDay(zone()).toInstant();

        DailyStats stats = new DailyStats();
        stats.setId(day.toString());
        stats.setComputedAt(Instant.now());

        stats.setVisits((int) distinctCount("sessionId", from, to));
        stats.setUniqueVisitors((int) distinctCount("anonId", from, to));

        Map<String, Integer> byType = countBy("$type", from, to, null);
        stats.setByType(byType);
        stats.setPageViews(byType.getOrDefault(EventType.PAGE_VIEW.name(), 0));
        stats.setByDevice(countBy("$device", from, to, null));
        stats.setByCity(countBy("$city", from, to, null));

        stats.setByChannel(channelBreakdown(from, to));
        stats.setTopProducts(topProducts(from, to));
        fillSearches(stats, from, to);

        stats.setFunnel(funnel(from, to, stats.getVisits()));

        DailyStats saved = repo.save(stats);
        log.info("جمع‌بندیِ {} ساخته شد: {} بازدید، {} بازدیدکنندهٔ یکتا، {} مشاهدهٔ صفحه.",
                day, saved.getVisits(), saved.getUniqueVisitors(), saved.getPageViews());
        return saved;
    }

    private long distinctCount(String field, Instant from, Instant to) {
        List<Document> pipeline = List.of(
                new Document("$match", range(from, to)),
                new Document("$group", new Document("_id", "$" + field)),
                new Document("$count", "n"));
        Document result = first(pipeline);
        return result == null ? 0 : ((Number) result.get("n")).longValue();
    }

    /** شمارشِ ساده به تفکیکِ یک فیلد؛ مقدارهای خالی کنار گذاشته می‌شوند. */
    private Map<String, Integer> countBy(String field, Instant from, Instant to, Document extraMatch) {
        Document match = range(from, to);
        if (extraMatch != null) match.putAll(extraMatch);
        List<Document> pipeline = List.of(
                new Document("$match", match),
                new Document("$group", new Document("_id", field).append("n", new Document("$sum", 1))),
                new Document("$sort", new Document("n", -1)));

        Map<String, Integer> out = new LinkedHashMap<>();
        for (Document d : aggregate(pipeline)) {
            Object key = d.get("_id");
            if (key == null) continue;
            out.put(String.valueOf(key), ((Number) d.get("n")).intValue());
        }
        return out;
    }

    /**
     * تفکیکِ کانال با نرخِ تبدیل.
     * <p>
     * 🔴 بازدید اینجا {@code sessionId}ِ یکتاست، نه تعدادِ رویداد: کانالی که ترافیکِ
     * زیاد و سفارشِ صفر می‌آورد باید دیده شود، و اگر مخرج تعدادِ رویداد باشد نرخِ
     * تبدیل بی‌معنی می‌شود.
     */
    private Map<String, DailyStats.ChannelStats> channelBreakdown(Instant from, Instant to) {
        List<Document> pipeline = List.of(
                new Document("$match", range(from, to)),
                new Document("$group", new Document("_id",
                        new Document("channel", "$channel").append("session", "$sessionId"))
                        .append("productViews", sumIfType(EventType.PRODUCT_VIEW))
                        .append("addToCart", sumIfType(EventType.ADD_TO_CART))
                        .append("orders", sumIfType(EventType.ORDER_PLACED))),
                new Document("$group", new Document("_id", "$_id.channel")
                        .append("visits", new Document("$sum", 1))
                        .append("productViews", new Document("$sum", "$productViews"))
                        .append("addToCart", new Document("$sum", "$addToCart"))
                        .append("orders", new Document("$sum", "$orders"))),
                new Document("$sort", new Document("visits", -1)));

        Map<String, DailyStats.ChannelStats> out = new LinkedHashMap<>();
        for (Document d : aggregate(pipeline)) {
            Object key = d.get("_id");
            if (key == null) continue;
            DailyStats.ChannelStats cs = new DailyStats.ChannelStats();
            cs.setVisits(intOf(d, "visits"));
            cs.setProductViews(intOf(d, "productViews"));
            cs.setAddToCart(intOf(d, "addToCart"));
            cs.setOrders(intOf(d, "orders"));
            out.put(String.valueOf(key), cs);
        }
        return out;
    }

    /**
     * قیف بر حسبِ <b>بازدید</b>، نه تعدادِ رویداد.
     * <p>
     * ⚠️ این تفاوت ظریف ولی تعیین‌کننده است: اگر پلهٔ «مشاهدهٔ محصول» تعدادِ رویداد
     * باشد، از پلهٔ «بازدید» بزرگ‌تر درمی‌آید (هر بازدید چند محصول می‌بیند) و درصدِ
     * ریزش منفی می‌شود — یعنی عددی که هیچ معنایی ندارد. قیف فقط وقتی معنا دارد که
     * هر پله <b>زیرمجموعهٔ</b> پلهٔ قبل باشد، پس اینجا می‌شماریم «چند بازدید به این
     * پله رسید».
     */
    private DailyStats.Funnel funnel(Instant from, Instant to, int visits) {
        List<Document> pipeline = List.of(
                new Document("$match", range(from, to)),
                new Document("$group", new Document("_id", "$sessionId")
                        .append("productViews", sumIfType(EventType.PRODUCT_VIEW))
                        .append("addToCart", sumIfType(EventType.ADD_TO_CART))
                        .append("beginCheckout", sumIfType(EventType.BEGIN_CHECKOUT))
                        .append("orders", sumIfType(EventType.ORDER_PLACED))),
                new Document("$group", new Document("_id", null)
                        .append("productViews", countIfPositive("$productViews"))
                        .append("addToCart", countIfPositive("$addToCart"))
                        .append("beginCheckout", countIfPositive("$beginCheckout"))
                        .append("orders", countIfPositive("$orders"))));

        DailyStats.Funnel f = new DailyStats.Funnel();
        f.setVisits(visits);
        Document d = first(pipeline);
        if (d != null) {
            f.setProductViews(intOf(d, "productViews"));
            f.setAddToCart(intOf(d, "addToCart"));
            f.setBeginCheckout(intOf(d, "beginCheckout"));
            f.setOrders(intOf(d, "orders"));
        }
        return f;
    }

    /** «این بازدید حداقل یک‌بار این کار را کرد» — یک، نه تعدادِ دفعات. */
    private Document countIfPositive(String field) {
        return new Document("$sum", new Document("$cond",
                List.of(new Document("$gt", List.of(field, 0)), 1, 0)));
    }

    private Document sumIfType(EventType type) {
        return new Document("$sum", new Document("$cond",
                List.of(new Document("$eq", List.of("$type", type.name())), 1, 0)));
    }

    private List<DailyStats.TopProduct> topProducts(Instant from, Instant to) {
        Document match = range(from, to);
        match.append("type", EventType.PRODUCT_VIEW.name());
        match.append("entityId", new Document("$ne", null));

        List<Document> pipeline = List.of(
                new Document("$match", match),
                new Document("$group", new Document("_id", "$entityId")
                        .append("n", new Document("$sum", 1))
                        // اسنپ‌شاتِ نام: اگر محصول بعداً حذف شود، گزارش بی‌نام نشود
                        .append("name", new Document("$last", "$entityName"))),
                new Document("$sort", new Document("n", -1)),
                new Document("$limit", TOP_PRODUCTS));

        List<DailyStats.TopProduct> out = new ArrayList<>();
        for (Document d : aggregate(pipeline)) {
            out.add(new DailyStats.TopProduct(
                    String.valueOf(d.get("_id")),
                    d.getString("name"),
                    intOf(d, "n")));
        }
        return out;
    }

    /**
     * عبارت‌های جست‌وجو، و جداگانه آن‌هایی که نتیجه‌ای نداشتند.
     * <p>
     * جست‌وجوی بی‌نتیجه ارزشمندترین دادهٔ این سامانه است: دقیقاً می‌گوید مشتری دنبالِ
     * چه چیزی آمده که ما نداریم.
     */
    private void fillSearches(DailyStats stats, Instant from, Instant to) {
        Document match = range(from, to);
        match.append("type", EventType.SEARCH.name());
        match.append("props.q", new Document("$ne", null));

        List<Document> pipeline = List.of(
                new Document("$match", match),
                new Document("$group", new Document("_id", "$props.q")
                        .append("n", new Document("$sum", 1))
                        .append("avgResults", new Document("$avg",
                                new Document("$ifNull", List.of("$props.resultCount", 0))))),
                new Document("$sort", new Document("n", -1)),
                new Document("$limit", TOP_SEARCHES));

        List<DailyStats.SearchTerm> top = new ArrayList<>();
        List<DailyStats.SearchTerm> zero = new ArrayList<>();
        for (Document d : aggregate(pipeline)) {
            Object term = d.get("_id");
            if (term == null) continue;
            double avg = d.get("avgResults") == null ? 0 : ((Number) d.get("avgResults")).doubleValue();
            DailyStats.SearchTerm st = new DailyStats.SearchTerm(String.valueOf(term), intOf(d, "n"), avg);
            top.add(st);
            if (avg == 0) zero.add(st);
        }
        stats.setTopSearches(top);
        stats.setZeroSearches(zero);
    }

    private Document range(Instant from, Instant to) {
        return new Document("at", new Document("$gte", from).append("$lt", to));
    }

    private List<Document> aggregate(List<Document> pipeline) {
        return mongo.getCollection("user_events")
                .aggregate(pipeline).into(new ArrayList<>());
    }

    private Document first(List<Document> pipeline) {
        List<Document> all = aggregate(pipeline);
        return all.isEmpty() ? null : all.get(0);
    }

    private int intOf(Document d, String key) {
        Object v = d.get(key);
        return v == null ? 0 : ((Number) v).intValue();
    }
}
