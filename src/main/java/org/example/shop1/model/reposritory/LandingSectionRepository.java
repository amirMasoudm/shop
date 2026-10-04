package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.LandingSection;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface LandingSectionRepository extends MongoRepository<LandingSection, String> {
    List<LandingSection> findByIsActiveOrderByOrderIndexAsc(boolean isActive);
}