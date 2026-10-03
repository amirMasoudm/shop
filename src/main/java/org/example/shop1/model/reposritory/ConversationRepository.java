package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.Conversation;
import org.example.shop1.model.enums.ConversationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {

    Optional<Conversation> findByCustomerId(String customerId);

    /** صفِ مشترک: تصاحب‌نشده‌ها. مرتب‌سازی بر اساسِ قدیمی‌ترین انتظار در سرویس انجام می‌شود. */
    List<Conversation> findByAssignedAgentIdIsNullAndStatus(ConversationStatus status);

    /** «چت‌های من» — گفت‌وگوهای همین کارشناس. */
    List<Conversation> findByAssignedAgentIdOrderByLastMessageAtDesc(String assignedAgentId);

    List<Conversation> findAllByOrderByLastMessageAtDesc();
}
