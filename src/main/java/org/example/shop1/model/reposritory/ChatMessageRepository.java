package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.ChatMessage;
import org.example.shop1.model.enums.SenderRole;
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

    /**
     * آخرین پیامِ یک نقش در گفت‌وگو (با {@code PageRequest.of(0, 1)}).
     * <p>
     * برایِ پیداکردنِ «آخرین پاسخِ کارشناس» است: هرچه مشتری بعد از آن فرستاده،
     * بی‌جواب مانده. عمداً فهرست برمی‌گرداند نه {@code Optional}، چون
     * {@code findFirst…} در Spring Data نمی‌تواند با {@code Pageable} ترکیب شود و
     * نسخهٔ فهرستی اینجا خواناتر از یک کوئریِ دستی است.
     */
    List<ChatMessage> findByConversationIdAndSenderRoleOrderByCreatedAtDesc(
            String conversationId, SenderRole senderRole, Pageable pageable);

    /** همهٔ پیام‌های یک نقش — وقتی کارشناس هنوز هیچ پاسخی نداده است. */
    List<ChatMessage> findByConversationIdAndSenderRoleOrderByCreatedAtAsc(
            String conversationId, SenderRole senderRole);

    /** پیام‌های یک نقش بعد از یک لحظه — یعنی بعد از آخرین پاسخِ کارشناس. */
    List<ChatMessage> findByConversationIdAndSenderRoleAndCreatedAtAfterOrderByCreatedAtAsc(
            String conversationId, SenderRole senderRole, Instant after);
}
