package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.EducationArchiveItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface EducationArchiveItemRepository extends MongoRepository<EducationArchiveItem, String> {
    List<EducationArchiveItem> findByIsActiveOrderByOrderIndexAsc(boolean isActive);
}
