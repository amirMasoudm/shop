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

    public ImageRowBanner save(ImageRowBanner input) {
        if (input.getTitle() == null || input.getTitle().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "عنوانِ پنجره الزامی است");
        }
        if (input.getPlacement() == null || input.getPlacement().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "جایگاهِ نمایش را انتخاب کنید");
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
