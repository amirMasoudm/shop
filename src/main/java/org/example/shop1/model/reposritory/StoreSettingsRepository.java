package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.StoreSettings;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreSettingsRepository extends MongoRepository<StoreSettings, String> {
}