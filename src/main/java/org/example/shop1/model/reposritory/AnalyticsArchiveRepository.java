package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.AnalyticsArchive;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AnalyticsArchiveRepository extends MongoRepository<AnalyticsArchive, String> {

    List<AnalyticsArchive> findAllByOrderByCreatedAtDesc();
}
