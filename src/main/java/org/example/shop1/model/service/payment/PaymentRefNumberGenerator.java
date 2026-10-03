package org.example.shop1.model.service.payment;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * تولیدِ {@code orderId}ِ عددیِ یکتا برایِ درگاهِ ملت.
 * <p>
 * {@code Order.orderCode} فعلی ("ORD-" + currentTimeMillis % 1000000) برایِ این کار امن نیست:
 * هر ~۱۶.۷ دقیقه دور می‌زند و هیچ تضمینِ اتمیکی ندارد — دو سفارشِ هم‌زمان می‌توانند عددِ
 * یکسان بگیرند. اینجا با {@code findAndModify}ِ اتمیکِ Mongo رویِ یک کالکشنِ counter (نه
 * {@code currentTimeMillis}) یک عددِ صحیح و همیشه-افزایشی می‌سازیم — حتی زیرِ بارِ هم‌زمان
 * هم دو درخواست هرگز یک عدد نمی‌گیرند (تضمینِ خودِ Mongo روی findAndModify).
 */
@Component
public class PaymentRefNumberGenerator {

    private static final String COUNTERS_COLLECTION = "counters";
    private static final String SEQUENCE_ID = "order_payment_ref";

    private final MongoTemplate mongoTemplate;

    public PaymentRefNumberGenerator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public long next() {
        Query query = Query.query(where("_id").is(SEQUENCE_ID));
        Update update = new Update().inc("seq", 1);
        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true).upsert(true);

        CounterDoc result = mongoTemplate.findAndModify(query, update, options, CounterDoc.class, COUNTERS_COLLECTION);
        if (result == null) {
            // عملاً نباید برسیم اینجا (upsert همیشه سند برمی‌گرداند)؛ فقط برایِ سیگنالِ روشن
            throw new IllegalStateException("تولیدِ شمارهٔ پرداخت ناموفق بود");
        }
        return result.seq;
    }

    /** فقط شکلِ minimal سندِ کالکشنِ counters — نیازی به entity/repository کامل نیست. */
    static class CounterDoc {
        String id;
        long seq;
    }
}
