package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * جمع‌بندیِ یک روز.
 * <p>
 * 🔴 <b>این سند هرگز پاک نمی‌شود</b> — همین چیزی است که نمودارِ چندساله را بعد از
 * آرشیو و حذفِ دادهٔ خام زنده نگه می‌دارد. حجمش ناچیز است (یک سند در روز).
 * <p>
 * داشبورد فقط از اینجا می‌خواند، نه از {@code user_events}. اگر نماهای کلی روی
 * دادهٔ خام کوئری بزنند، ماهِ ششم باز نمی‌شوند.
 */
@Document(collection = "daily_stats")
public class DailyStats {

    /** تفکیکِ یک کانال — همان چیزی که «نرخِ تبدیلِ کانال» از آن درمی‌آید. */
    public static class ChannelStats {
        private int visits;
        private int productViews;
        private int addToCart;
        private int orders;

        public int getVisits() { return visits; }
        public void setVisits(int visits) { this.visits = visits; }

        public int getProductViews() { return productViews; }
        public void setProductViews(int productViews) { this.productViews = productViews; }

        public int getAddToCart() { return addToCart; }
        public void setAddToCart(int addToCart) { this.addToCart = addToCart; }

        public int getOrders() { return orders; }
        public void setOrders(int orders) { this.orders = orders; }
    }

    /** پله‌های قیف — درصدِ ریزش در پنل از روی همین‌ها حساب می‌شود. */
    public static class Funnel {
        private int visits;
        private int productViews;
        private int addToCart;
        private int beginCheckout;
        private int orders;

        public int getVisits() { return visits; }
        public void setVisits(int visits) { this.visits = visits; }

        public int getProductViews() { return productViews; }
        public void setProductViews(int productViews) { this.productViews = productViews; }

        public int getAddToCart() { return addToCart; }
        public void setAddToCart(int addToCart) { this.addToCart = addToCart; }

        public int getBeginCheckout() { return beginCheckout; }
        public void setBeginCheckout(int beginCheckout) { this.beginCheckout = beginCheckout; }

        public int getOrders() { return orders; }
        public void setOrders(int orders) { this.orders = orders; }
    }

    public static class TopProduct {
        private String entityId;
        private String entityName;
        private int views;

        public TopProduct() {}
        public TopProduct(String entityId, String entityName, int views) {
            this.entityId = entityId; this.entityName = entityName; this.views = views;
        }

        public String getEntityId() { return entityId; }
        public void setEntityId(String entityId) { this.entityId = entityId; }

        public String getEntityName() { return entityName; }
        public void setEntityName(String entityName) { this.entityName = entityName; }

        public int getViews() { return views; }
        public void setViews(int views) { this.views = views; }
    }

    public static class SearchTerm {
        private String term;
        private int count;
        private double avgResults;

        public SearchTerm() {}
        public SearchTerm(String term, int count, double avgResults) {
            this.term = term; this.count = count; this.avgResults = avgResults;
        }

        public String getTerm() { return term; }
        public void setTerm(String term) { this.term = term; }

        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }

        public double getAvgResults() { return avgResults; }
        public void setAvgResults(double avgResults) { this.avgResults = avgResults; }
    }

    /** تاریخِ روز به قالبِ {@code YYYY-MM-DD} — همین کلید، بازاجرای جاب را idempotent می‌کند. */
    @Id
    private String id;

    private int visits;
    private int uniqueVisitors;
    private int pageViews;

    private Map<String, Integer> byType = new LinkedHashMap<>();
    private Map<String, ChannelStats> byChannel = new LinkedHashMap<>();
    private Map<String, Integer> byCity = new LinkedHashMap<>();
    private Map<String, Integer> byDevice = new LinkedHashMap<>();

    private List<TopProduct> topProducts = List.of();
    private List<SearchTerm> topSearches = List.of();
    private List<SearchTerm> zeroSearches = List.of();

    private Funnel funnel = new Funnel();

    private Instant computedAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getVisits() { return visits; }
    public void setVisits(int visits) { this.visits = visits; }

    public int getUniqueVisitors() { return uniqueVisitors; }
    public void setUniqueVisitors(int uniqueVisitors) { this.uniqueVisitors = uniqueVisitors; }

    public int getPageViews() { return pageViews; }
    public void setPageViews(int pageViews) { this.pageViews = pageViews; }

    public Map<String, Integer> getByType() { return byType; }
    public void setByType(Map<String, Integer> byType) { this.byType = byType; }

    public Map<String, ChannelStats> getByChannel() { return byChannel; }
    public void setByChannel(Map<String, ChannelStats> byChannel) { this.byChannel = byChannel; }

    public Map<String, Integer> getByCity() { return byCity; }
    public void setByCity(Map<String, Integer> byCity) { this.byCity = byCity; }

    public Map<String, Integer> getByDevice() { return byDevice; }
    public void setByDevice(Map<String, Integer> byDevice) { this.byDevice = byDevice; }

    public List<TopProduct> getTopProducts() { return topProducts; }
    public void setTopProducts(List<TopProduct> topProducts) { this.topProducts = topProducts; }

    public List<SearchTerm> getTopSearches() { return topSearches; }
    public void setTopSearches(List<SearchTerm> topSearches) { this.topSearches = topSearches; }

    public List<SearchTerm> getZeroSearches() { return zeroSearches; }
    public void setZeroSearches(List<SearchTerm> zeroSearches) { this.zeroSearches = zeroSearches; }

    public Funnel getFunnel() { return funnel; }
    public void setFunnel(Funnel funnel) { this.funnel = funnel; }

    public Instant getComputedAt() { return computedAt; }
    public void setComputedAt(Instant computedAt) { this.computedAt = computedAt; }
}
