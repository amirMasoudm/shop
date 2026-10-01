package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.AppRegistration;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AppRegistrationRepository extends MongoRepository<AppRegistration, String> {
}
