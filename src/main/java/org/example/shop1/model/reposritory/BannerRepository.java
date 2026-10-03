package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.Banner;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface BannerRepository extends MongoRepository<Banner, String> {
    List<Banner> findByIsActiveOrderByOrderIndexAsc(boolean isActive);
}
