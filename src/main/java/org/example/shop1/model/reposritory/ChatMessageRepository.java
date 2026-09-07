package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    /** صفحه‌بندیِ تاریخچه با اسکرول به بالا: قدیمی‌تر از یک لحظهٔ مشخص. */
    List<ChatMessage> findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(
            String conversationId, Instant before, Pageable pageable);

    /** آخرین صفحه (وقتی کلاینت هنوز نقطهٔ شروعی ندارد). */
    List<ChatMessage> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);

    /**
     * جبرانِ قطعیِ وب‌سوکت: هرچه بعد از آخرین پیامِ دیده‌شده آمده.
     * ترتیب صعودی است چون کلاینت همان‌طور به انتهای گفت‌وگو اضافه‌شان می‌کند.
     */
    List<ChatMessage> findByConversationIdAndCreatedAtAfterOrderByCreatedAtAsc(
            String conversationId, Instant after);

    long countByConversationId(String conversationId);
}
