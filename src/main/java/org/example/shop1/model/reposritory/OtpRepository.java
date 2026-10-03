package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.OtpData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpRepository extends MongoRepository<OtpData, String> {
}
