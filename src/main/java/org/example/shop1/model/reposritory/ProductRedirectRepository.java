package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.ProductRedirect;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRedirectRepository extends MongoRepository<ProductRedirect, String> {

    Optional<ProductRedirect> findByFromSlug(String fromSlug);

    boolean existsByFromSlug(String fromSlug);

    List<ProductRedirect> findAllByOrderByCreatedAtDesc();
}
