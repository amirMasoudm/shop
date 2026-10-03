package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.Campaign;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CampaignRepository extends MongoRepository<Campaign, String> {

    Optional<Campaign> findByCode(String code);

    Optional<Campaign> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByCode(String code);

    List<Campaign> findAllByOrderByCreatedAtDesc();
}
