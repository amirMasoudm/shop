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

    Page<ActivityLog> findByAtBetweenOrderByAtDesc(Instant from, Instant to, Pageable pageable);

    Page<ActivityLog> findByUsernameAndAtBetweenOrderByAtDesc(
            String username, Instant from, Instant to, Pageable pageable);

    // فیلترِ نوعِ موجودیت — پایه‌ی دوقسمتی‌شدنِ صفحه‌ی لاگ
    Page<ActivityLog> findByEntityTypeAndAtBetweenOrderByAtDesc(
            String entityType, Instant from, Instant to, Pageable pageable);

    Page<ActivityLog> findByUsernameAndEntityTypeAndAtBetweenOrderByAtDesc(
            String username, String entityType, Instant from, Instant to, Pageable pageable);
}
