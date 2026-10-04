package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.HolooCode;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HolooCodeRepository extends MongoRepository<HolooCode, String> {

    Optional<HolooCode> findByCode(String code);

    boolean existsByCode(String code);

    /** فهرستِ رزروهایِ بی‌صاحب — «کد گرفتم و یادم رفت» بدونِ این دیده نمی‌شود. */
    List<HolooCode> findByStatusOrderByIssuedAtDesc(HolooCode.Status status);

    long countByStatus(HolooCode.Status status);
}
