package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.AppPendingRegistration;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AppPendingRegistrationRepository extends MongoRepository<AppPendingRegistration, String> {
}
