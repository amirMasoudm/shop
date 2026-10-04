package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * ⚠️ عمداً هیچ متدِ حذفی اینجا اضافه نشود — لاگ append-only است.
 * (متدهای حذفِ ارثی از MongoRepository در هیچ کنترلری expose نشده‌اند.)
 */
@Repository
public interface ActivityLogRepository extends MongoRepository<ActivityLog, String> {

    // ℹ️ متدهایِ مشتق‌شدهٔ جست‌وجو برداشته شدند و جایشان یک کوئریِ پویا در
    // ActivityLogService نشست. دلیل: با اضافه‌شدنِ فیلترِ «رویداد» به کاربر/نوع/بازهٔ
    // زمانی، تعدادِ ترکیب‌ها از ۴ به ۸ می‌رسید و هر فیلترِ بعدی دوباره دو برابرش
    // می‌کرد — یعنی یک متدِ تازه به‌ازای هر ترکیب، نه به‌ازای هر فیلتر.
}
