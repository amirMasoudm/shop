package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.DailyStats;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface DailyStatsRepository extends MongoRepository<DailyStats, String> {

    /**
     * شناسه خودِ تاریخِ {@code YYYY-MM-DD} است، پس مرتب‌سازیِ رشته‌ای همان ترتیبِ زمانی است.
     * <p>
     * ⚠️ عمداً {@code @Query} صریح است و نه {@code findByIdBetween}: مشتقِ
     * {@code Between} در Spring Data Mongo <b>دو سرِ بازه را حذف می‌کند</b>، یعنی
     * بازهٔ یک‌روزه ({@code from == to}) همیشه خالی برمی‌گشت و نمودار آن روز را
     * نداشت. اینجا هر دو سر شامل‌اند.
     */
    @Query("{ '_id': { $gte: ?0, $lte: ?1 } }")
    List<DailyStats> findRange(String from, String to, Sort sort);
}
