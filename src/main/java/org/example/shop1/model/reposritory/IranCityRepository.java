package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.IranCity;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface IranCityRepository extends MongoRepository<IranCity, String> {
    Optional<IranCity> findByName(String name);
}