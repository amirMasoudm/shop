package org.example.shop1.model.service;

import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.ImageRowBanner;
import org.example.shop1.model.reposritory.ImageRowBannerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImageRowBannerService {

    private final ImageRowBannerRepository repo;

    public ImageRowBannerService(ImageRowBannerRepository repo) {
        this.repo = repo;
    }

    public List<ImageRowBanner> getAllForAdmin() {
        return repo.findAll();
    }

    public List<ImageRowBanner> getActiveByPlacement(String placement) {
        return repo.findByPlacementAndActiveTrueOrderByPositionAsc(placement);
    }

    /** همه‌ی پنجره‌های فعال، هر جایگاهی — برایِ رندرِ کلاینتیِ SHOP_AFTER_SECTION
     * (مثلِ BannerService.getActiveBannersResolved که همه را می‌دهد و کلاینت فیلتر می‌کند). */
    public List<ImageRowBanner> getAllActive() {
        return repo.findByActiveTrueOrderByPositionAsc();
    }

    public ImageRowBanner save(ImageRowBanner input) {
        if (input.getTitle() == null || input.getTitle().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "عنوانِ پنجره الزامی است");
        }
        if (input.getPlacement() == null || input.getPlacement().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "جایگاهِ نمایش را انتخاب کنید");
        }
        if ("SHOP_AFTER_SECTION".equals(input.getPlacement())
                && (input.getAfterSectionId() == null || input.getAfterSectionId().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "برایِ نمایش زیرِ یک سکشن، باید همان سکشن را انتخاب کنید");
        }
        if (!"SHOP_AFTER_SECTION".equals(input.getPlacement())) {
            input.setAfterSectionId(null);
        }
        if (input.getDisplayMode() == null || input.getDisplayMode().isBlank()) {
            input.setDisplayMode("STATIC");
        }
        if (input.getRotationSpeedSeconds() <= 0) {
            input.setRotationSpeedSeconds(5);
        }
        return repo.save(input);
    }

    public void delete(String id) {
        if (!repo.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "پنجره یافت نشد");
        }
        repo.deleteById(id);
    }
}
