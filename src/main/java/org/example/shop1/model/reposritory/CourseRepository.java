package org.example.shop1.model.reposritory;

import org.example.shop1.model.entity.Course;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CourseRepository extends MongoRepository<Course, String> {
    Optional<Course> findBySlug(String slug);
}
