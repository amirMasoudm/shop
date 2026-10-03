package org.example.shop1.controller;

import org.example.shop1.model.entity.Course;
import org.example.shop1.model.service.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** الگویِ دقیقاً مشابهِ ArticleController — CRUDِ ادمین + اندپوینت‌هایِ عمومیِ خواندنی. */
@RestController
@RequestMapping("/api/v1/courses")
@CrossOrigin
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    // --- عمومی (/learn) ---
    @GetMapping("/active")
    public ResponseEntity<List<Course>> getAllForLearnPage() {
        return ResponseEntity.ok(courseService.getAllForLearnPage());
    }

    @GetMapping("/single/{slugOrId}")
    public ResponseEntity<Course> getOne(@PathVariable String slugOrId) {
        return courseService.findBySlugOrId(slugOrId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "دوره یافت نشد"));
    }

    // --- ادمین (Admin.html) ---
    @GetMapping("/admin/all")
    public ResponseEntity<List<Course>> getAllForAdmin() {
        return ResponseEntity.ok(courseService.getAllForAdmin());
    }

    @PostMapping("/admin")
    public ResponseEntity<Course> save(@RequestBody Course course) {
        return ResponseEntity.ok(courseService.save(course));
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
