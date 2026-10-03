package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.AppToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AppTokenRepository extends MongoRepository<AppToken, String> {
    Optional<AppToken> findByTokenHash(String tokenHash);
    List<AppToken> findByInstallationIdAndActiveTrue(String installationId);
}
