package org.example.shop1.model.service;

import org.example.shop1.model.entity.RateCounter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * محدودیتِ نرخِ پنجره‌ای روی مونگو.
 * <p>
 * هر کلید یک پنجره دارد که با <b>اولین</b> برخورد شروع می‌شود و تا {@code window} بعد
 * می‌ماند؛ پس «فاصلهٔ ۶۰ ثانیه» واقعاً ۶۰ ثانیه از آخرین ارسال است، نه تا سرِ دقیقهٔ
 * بعد. شمارش اتمیک است ({@code findAndModify})، تا دو درخواستِ هم‌زمان هر دو «اولی» نشوند.
 * <p>
 * ⚠️ هر برخورد شمرده می‌شود، حتی اگر کلیدِ دیگری بعدش درخواست را رد کند. این عمدی
 * است: شمردنِ درخواستِ ردشده سخت‌گیرانه‌تر است، و برای مسیری که پول (پیامک) خرج
 * می‌کند سخت‌گیری بهتر از نشت است.
 */
@Service
public class RateLimitService {

    private final MongoTemplate mongo;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public RateLimitService(MongoTemplate mongo) {
        this(mongo, Clock.systemUTC());
    }

    RateLimitService(MongoTemplate mongo, Clock clock) {
        this.mongo = mongo;
        this.clock = clock;
    }

    /** نتیجهٔ یک برخورد: شمارِ تازه، و چند ثانیه تا بازشدنِ پنجره. */
    public record Hit(long count, long limit, long retryAfterSec) {
        public boolean exceeded() { return count > limit; }
    }

    public Hit hit(String key, long limit, Duration window) {
        for (int attempt = 0; attempt < 3; attempt++) {
            Instant now = clock.instant();

            // ۱) پنجرهٔ زنده: فقط یکی اضافه کن
            RateCounter live = mongo.findAndModify(
                    Query.query(where("_id").is(key).and("expireAt").gt(now)),
                    new Update().inc("count", 1),
                    FindAndModifyOptions.options().returnNew(true),
                    RateCounter.class);
            if (live != null) return toHit(live, limit, now);

            // ۲) نبود یا منقضی بود: پنجرهٔ تازه. اگر هم‌زمان کسی پنجرهٔ زنده ساخت،
            //    upsert با کلیدِ تکراری می‌خورد و دور بعد به شاخهٔ ۱ می‌رسیم.
            try {
                RateCounter fresh = mongo.findAndModify(
                        Query.query(where("_id").is(key).and("expireAt").lte(now)),
                        new Update().set("count", 1L).set("expireAt", now.plus(window)),
                        FindAndModifyOptions.options().returnNew(true).upsert(true),
                        RateCounter.class);
                if (fresh != null) return toHit(fresh, limit, now);
            } catch (DuplicateKeyException race) {
                // دورِ بعد
            }
        }
        // سه بار مسابقه باختن عملاً ناممکن است؛ اگر شد، محتاطانه رد کن.
        return new Hit(limit + 1, limit, window.toSeconds());
    }

    /** شمارِ فعلیِ یک کلید بدونِ افزودن — برای هشدارِ سقفِ روزانه. */
    public long current(String key) {
        RateCounter c = mongo.findOne(
                Query.query(where("_id").is(key).and("expireAt").gt(clock.instant())), RateCounter.class);
        return c == null ? 0 : c.getCount();
    }

    private static Hit toHit(RateCounter c, long limit, Instant now) {
        long left = Math.max(1, (long) Math.ceil(Duration.between(now, c.getExpireAt()).toMillis() / 1000.0));
        return new Hit(c.getCount(), limit, left);
    }
}
