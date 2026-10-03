package org.example.shop1.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * ایندکس‌های کالکشن‌های {@code /api/app/v1}. auto-index-creation در این پروژه عمداً خاموش
 * است (شرحش در {@link MongoIndexInitializer})، پس بدونِ این کلاس هیچ‌کدام از قیدهای زیر
 * وجود نداشت — نه یکتاییِ هشِ توکن، نه پاک‌شدنِ خودکارِ شمارنده‌ها.
 * <p>
 * این کالکشن‌ها تازه‌اند و دادهٔ تکراریِ قدیمی ندارند، پس بررسیِ پیش از ساختِ
 * {@code MongoIndexInitializer} این‌جا لازم نیست. خطا برنامه را پایین نمی‌آورد، فقط لاگ.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AppApiIndexes implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AppApiIndexes.class);

    private final MongoTemplate mongo;

    public AppApiIndexes(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void run(String... args) {
        ensure("app_tokens", new Index().on("tokenHash", Sort.Direction.ASC).unique().named("uk_app_tokens_hash"));
        // یک توکنِ فعال برای هر دستگاه — قیدِ دیتابیسی، نه فقط قولِ کد.
        ensure("app_tokens", new Index().on("installationId", Sort.Direction.ASC).unique()
                .partial(PartialIndexFilter.of(where("active").is(true))).named("uk_app_tokens_active_installation"));
        ensure("app_tokens", new Index().on("userId", Sort.Direction.ASC).named("ix_app_tokens_user"));
        ensure("app_registrations", new Index().on("createdAt", Sort.Direction.DESC).named("ix_app_registrations_at"));
        ensure("app_pending_registrations", new Index().on("createdAt", Sort.Direction.ASC)
                .expire(Duration.ofMinutes(10)).named("ttl_app_pending_10m"));
        ensure("rate_counters", new Index().on("expireAt", Sort.Direction.ASC)
                .expire(Duration.ZERO).named("ttl_rate_counters"));
    }

    private void ensure(String collection, Index index) {
        try {
            mongo.indexOps(collection).ensureIndex(index);
        } catch (RuntimeException e) {
            log.error("❌ ایندکسِ {} روی {} ساخته نشد: {}", index.getIndexOptions().get("name"), collection, e.getMessage());
        }
    }
}
