package org.example.shop1.model.service;

import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.EducationArchiveItem;
import org.example.shop1.model.reposritory.EducationArchiveItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EducationArchiveService {

    private final EducationArchiveItemRepository repo;

    public EducationArchiveService(EducationArchiveItemRepository repo) {
        this.repo = repo;
    }

    public List<EducationArchiveItem> getAllForAdmin() {
        return repo.findAll();
    }

    public EducationArchiveItem save(EducationArchiveItem item) {
        if (item.getTitle() == null || item.getTitle().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "عنوانِ عکس الزامی است");
        }
        if (item.getImageUrls() == null || item.getImageUrls().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "حداقل یک عکس الزامی است");
        }
        return repo.save(item);
    }

    public void delete(String id) {
        repo.deleteById(id);
    }

    /** آیتم‌هایِ فعال، مرتب‌شده — هم برایِ پنجرهٔ صفحه‌ی داده‌نما، هم صفحه‌ی آموزشِ کامل. */
    public List<EducationArchiveItem> getActive() {
        return repo.findByIsActiveOrderByOrderIndexAsc(true);
    }
}
