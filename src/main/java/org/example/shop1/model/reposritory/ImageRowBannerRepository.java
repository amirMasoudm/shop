package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.ImageRowBanner;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ImageRowBannerRepository extends MongoRepository<ImageRowBanner, String> {
    List<ImageRowBanner> findByPlacementAndActiveTrueOrderByPositionAsc(String placement);
    List<ImageRowBanner> findByActiveTrueOrderByPositionAsc();
}
