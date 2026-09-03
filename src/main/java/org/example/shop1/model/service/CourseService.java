package org.example.shop1.model.service;

import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.Course;
import org.example.shop1.model.reposritory.CourseRepository;
import org.example.shop1.model.service.util.SlugUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    private final CourseRepository repo;

    public CourseService(CourseRepository repo) {
        this.repo = repo;
    }

    public List<Course> getAllForAdmin() {
        return repo.findAll();
    }

    /** برایِ /learn — همه‌ی دوره‌ها (فعال و غیرفعال)؛ وضعیت («باز»/«تکمیل»/«غیرفعال») سمتِ قالب محاسبه می‌شود. */
    public List<Course> getAllForLearnPage() {
        return repo.findAll();
    }

    public Optional<Course> findBySlugOrId(String slugOrId) {
        Optional<Course> found = repo.findBySlug(slugOrId);
        if (found.isEmpty()) found = repo.findById(slugOrId);
        return found;
    }

    public Course save(Course input) {
        boolean isNew = input.getId() == null || input.getId().isBlank();
        Course course;
        if (isNew) {
            course = new Course();
        } else {
            course = repo.findById(input.getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "دوره یافت نشد"));
        }

        if (input.getTitle() == null || input.getTitle().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "عنوانِ دوره الزامی است");
        }
        if (input.getPrice() == null || input.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "قیمتِ دوره الزامی است");
        }
        if (input.getCapacity() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ظرفیتِ دوره باید عددی بزرگ‌تر از صفر باشد");
        }

        course.setTitle(input.getTitle());
        course.setMode(input.getMode() != null ? input.getMode() : org.example.shop1.model.enums.CourseMode.IN_PERSON);
        course.setCategoryId(input.getCategoryId());
        course.setSyllabus(input.getSyllabus());
        course.setTheoryHours(input.getTheoryHours());
        course.setPracticalHours(input.getPracticalHours());
        course.setInstructor(input.getInstructor());
        course.setCapacity(input.getCapacity());
        // enrolledCount عمداً از ورودیِ ادمین گرفته نمی‌شود — فقط OrderService با خریدِ موفق زیادش می‌کند
        course.setLocation(input.getLocation());
        course.setStartDate(input.getStartDate());
        course.setEndDate(input.getEndDate());
        course.setImages(input.getImages());
        course.setBannerImage(input.getBannerImage());
        course.setPrice(input.getPrice());
        course.setActive(input.isActive());
        course.setInstructorDescription(input.getInstructorDescription());
        course.setOrganizerDescription(input.getOrganizerDescription());
        course.setFaqs(input.getFaqs());
        course.setSeoTitle(input.getSeoTitle());
        course.setSeoDescription(input.getSeoDescription());

        if (input.getSlug() != null && !input.getSlug().isBlank()) {
            course.setSlug(generateUniqueSlug(input.getSlug(), course.getId()));
        } else if (course.getSlug() == null || course.getSlug().isEmpty()) {
            course.setSlug(generateUniqueSlug(course.getTitle(), course.getId()));
        }

        return repo.save(course);
    }

    public void delete(String id) {
        if (!repo.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "دوره یافت نشد");
        }
        repo.deleteById(id);
    }

    private String generateUniqueSlug(String base, String excludeId) {
        String slug = SlugUtil.slugify(base);
        if (slug.isEmpty()) slug = "course";

        String candidate = slug;
        int i = 2;
        while (true) {
            Optional<Course> existing = repo.findBySlug(candidate);
            if (existing.isEmpty() || existing.get().getId().equals(excludeId)) {
                return candidate;
            }
            candidate = slug + "-" + i++;
        }
    }
}
