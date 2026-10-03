package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.LegacyRedirect;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LegacyRedirectRepository extends MongoRepository<LegacyRedirect, String> {

    Optional<LegacyRedirect> findByFromPath(String fromPath);

    boolean existsByFromPath(String fromPath);

    /** پرترافیک‌ترین‌ها اول — همان ترتیبی که در پنل تصمیم‌ساز است. */
    List<LegacyRedirect> findAllByOrderByHitsDescCreatedAtDesc();
}
