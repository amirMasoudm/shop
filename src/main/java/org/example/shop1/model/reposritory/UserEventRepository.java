package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.UserEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * ⚠️ نوشتنِ تکی از اینجا انجام نمی‌شود — نوشتن از صفِ {@code UserEventRecorder} و
 * به‌صورتِ دسته‌ای ({@code insertMany}) است تا مسیرِ درخواست کُند نشود.
 */
public interface UserEventRepository extends MongoRepository<UserEvent, String> {

    List<UserEvent> findBySessionIdOrderByAtAsc(String sessionId);

    List<UserEvent> findByAnonIdOrderByAtAsc(String anonId);

    long countByUserId(String userId);
}
