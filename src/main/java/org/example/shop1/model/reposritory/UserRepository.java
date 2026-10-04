package org.example.shop1.model.reposritory;



import org.example.shop1.model.entity.User;
import org.example.shop1.model.enums.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByPhoneNumber(String phoneNumber); // اضافه شد

    // exists* برخلاف findBy* روی رکوردهای تکراری خطا نمی‌دهد؛ برای چکِ یکتایی امن‌تر است
    boolean existsByUsername(String username);
    boolean existsByPhoneNumber(String phoneNumber);

    long countByRole(Role role);
    List<User> findByRole(Role role);
}
