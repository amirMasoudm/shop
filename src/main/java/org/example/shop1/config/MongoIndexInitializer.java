package org.example.shop1.config;

import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ایندکس‌های unique را هنگامِ بالا آمدنِ برنامه می‌سازد (کالکشن‌های users و orders).
 *
 * <p>چرا دستی و نه با {@code spring.data.mongodb.auto-index-creation=true}؟ چون آن گزینه
 * ایندکس را کورکورانه می‌سازد؛ اگر داده‌ی تکراری در دیتابیس باشد ساختِ ایندکس شکست می‌خورد و
 * برنامه با یک استک‌تریسِ نامفهوم بالا نمی‌آید. اینجا اول دنبالِ تکراری می‌گردیم و اگر بود،
 * دقیقاً می‌گوییم کدام مقدار تکراری است و چطور پاکش کند.
 *
 * <p>پیش‌زمینه: در ری‌استورِ دیتا رویِ سرور دو رکورد با {@code username: "superadmin"} ساخته شد و
 * {@code findByUsername} با {@code IncorrectResultSizeDataAccessException} می‌ترکید؛ یعنی کلِ
 * لاگینِ ادمین از کار می‌افتاد. {@code orders.paymentRefNumber} (شمارنده‌ی اتمیکِ درگاهِ ملت) هم
 * همین سطحِ محافظت را می‌گیرد — برخوردِ عددی روی پول واقعی است، نه یک کاربرِ گم‌شده.
 *
 * <p>سیاستِ خطا: اگر تکراری پیدا شود ایندکس ساخته <b>نمی‌شود</b> ولی برنامه هم بالا می‌آید — چون
 * پایین نگه داشتنِ کلِ سایت به‌خاطرِ یک ایندکس بدتر از بالا آمدن با یک لاگِ ERROR است. لاگ را
 * جدی بگیر: تا وقتی تکراری هست، همان باگِ لاگین/پرداخت سرِ جایش است.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // باید قبل از DataInitializer اجرا شود
public class MongoIndexInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexInitializer.class);

    /** حداکثر تعداد مقدارِ تکراری که در لاگ نشان می‌دهیم (که لاگ را غرق نکند). */
    private static final int MAX_REPORTED_DUPLICATES = 20;

    /**
     * یک فیلد و نامِ ایندکسِ uniqueـش در یک کالکشنِ مشخص.
     * {@code bsonType} باید با {@code $type} مچ شود ("string" یا "number") — چون partialFilterExpression
     * فقط با همین چک است که رکوردهایِ بدونِ‌مقدار (null/نبود) از قاعده‌ی unique معاف می‌مانند.
     */
    private record UniqueField(String collection, String field, String indexName, String bsonType) {}

    private static final List<UniqueField> UNIQUE_FIELDS = List.of(
            new UniqueField("users", "username", "uk_users_username", "string"),
            new UniqueField("users", "phoneNumber", "uk_users_phoneNumber", "string"),
            // paymentRefNumber فقط بعدِ شروعِ پرداخت (bpPayRequest) پر می‌شود؛ سفارش‌هایِ
            // درفت/پرداخت‌نشده مقدارش را ندارند و باید از قاعده‌ی unique معاف بمانند.
            new UniqueField("orders", "paymentRefNumber", "uk_orders_paymentRefNumber", "number")
    );

    private final MongoTemplate mongoTemplate;

    public MongoIndexInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        for (UniqueField uniqueField : UNIQUE_FIELDS) {
            MongoCollection<Document> collection = mongoTemplate.getCollection(uniqueField.collection());
            ensureUniqueIndex(collection, uniqueField);
        }
    }

    private void ensureUniqueIndex(MongoCollection<Document> collection, UniqueField uniqueField) {
        String field = uniqueField.field();
        String coll = uniqueField.collection();

        List<Document> duplicates = findDuplicates(collection, field, uniqueField.bsonType());
        if (!duplicates.isEmpty()) {
            log.error("""
                    ❌ ایندکسِ unique رویِ {}.{} ساخته نشد چون داده‌ی تکراری وجود دارد: {}
                       تا وقتی این‌ها پاک نشوند، findBy{} با IncorrectResultSizeDataAccessException می‌ترکد.
                       برای دیدنِ رکوردها به‌همراهِ _id، این را در mongosh بزن:
                       {}
                       رکوردهای اضافی را حذف کن و برنامه را دوباره بالا بیاور.""",
                    coll, field, describe(duplicates), capitalize(field), duplicateFinderCommand(coll, field, uniqueField.bsonType()));
            return;
        }

        try {
            // createIndex اگر ایندکس با همین نام و همین مشخصات موجود باشد بی‌اثر است (idempotent).
            collection.createIndex(
                    new Document(field, 1),
                    new IndexOptions()
                            .name(uniqueField.indexName())
                            .unique(true)
                            // بدونِ این فیلتر، رکوردهای بدونِ مقدار همگی «null» حساب می‌شوند
                            // و فقط یکی‌شان اجازه‌ی وجود دارد.
                            .partialFilterExpression(typedOnly(field, uniqueField.bsonType())));
            log.info("✅ ایندکسِ unique رویِ {}.{} آماده است ({}).", coll, field, uniqueField.indexName());
        } catch (MongoCommandException e) {
            // معمولاً یعنی از قبل ایندکسی رویِ همین فیلد با مشخصاتِ متفاوت ساخته شده.
            log.error("""
                    ❌ ساختِ ایندکسِ {} رویِ {}.{} شکست خورد: {}
                       اگر از قبل ایندکسِ دیگری رویِ این فیلد هست، در mongosh حذفش کن و دوباره بالا بیاور:
                       db.{}.getIndexes()   سپس   db.{}.dropIndex("<نامِ ایندکسِ قدیمی>")""",
                    uniqueField.indexName(), coll, field, e.getErrorMessage(), coll, coll);
        }
    }

    /** مقادیری که بیش از یک رکورد دارند، به‌همراه تعدادشان. */
    private List<Document> findDuplicates(MongoCollection<Document> collection, String field, String bsonType) {
        List<Document> pipeline = List.of(
                // فقط رکوردهایی که واقعاً مقدار دارند — دقیقاً همان چیزی که ایندکس پوشش می‌دهد.
                new Document("$match", typedOnly(field, bsonType)),
                new Document("$group", new Document("_id", "$" + field)
                        .append("count", new Document("$sum", 1))),
                new Document("$match", new Document("count", new Document("$gt", 1))),
                new Document("$sort", new Document("count", -1)),
                new Document("$limit", MAX_REPORTED_DUPLICATES));

        return collection.aggregate(pipeline).into(new ArrayList<>());
    }

    /**
     * دستورِ آماده‌ی mongosh برای دیدنِ رکوردهای تکراری به‌همراهِ _id هرکدام.
     * با concat ساخته می‌شود نه با placeholderهای لاگ، چون SLF4J براکتِ دوتایی را escape نمی‌کند
     * و {@code {}}‌های داخلِ JSON با placeholderها قاطی می‌شوند.
     */
    private static String duplicateFinderCommand(String collection, String field, String bsonType) {
        return "db." + collection + ".aggregate(["
                + "{$match:{" + field + ":{$type:\"" + bsonType + "\"}}},"
                + "{$group:{_id:\"$" + field + "\",ids:{$push:\"$_id\"},count:{$sum:1}}},"
                + "{$match:{count:{$gt:1}}}])";
    }

    private static Document typedOnly(String field, String bsonType) {
        // $type به‌جای $ne:null چون partialFilterExpression عملگرِ $ne را قبول نمی‌کند.
        return new Document(field, new Document("$type", bsonType));
    }

    private static String describe(List<Document> duplicates) {
        return duplicates.stream()
                .map(d -> "\"" + d.get("_id") + "\" × " + d.get("count"))
                .collect(Collectors.joining("، "));
    }

    private static String capitalize(String field) {
        return Character.toUpperCase(field.charAt(0)) + field.substring(1);
    }
}
