package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.Rfq;
import org.example.shop1.model.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RfqRepository extends MongoRepository<Rfq, String> {

    List<Rfq> findByUserOrderByCreatedAtDesc(User user);

    List<Rfq> findAllByOrderByCreatedAtDesc();

    Optional<Rfq> findByCode(String code);
}
