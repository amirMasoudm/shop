package org.example.shop1.model.service;

import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.ImageRowBanner;
import org.example.shop1.model.entity.ImageRowItem;
import org.example.shop1.model.reposritory.ImageRowBannerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ImageRowBannerService {

    private final ImageRowBannerRepository repo;

    public ImageRowBannerService(ImageRowBannerRepository repo) {
        this.repo = repo;
    }

    public List<ImageRowBanner> getAllForAdmin() {
        return sortImagesWithin(repo.findAll());
    }

    public List<ImageRowBanner> getActiveByPlacement(String placement) {
        return sortImagesWithin(repo.findByPlacementAndActiveTrueOrderByPositionAsc(placement));
    }

    /** همه‌ی پنجره‌های فعال، هر جایگاهی — برایِ رندرِ کلاینتیِ SHOP_AFTER_SECTION
     * (مثلِ BannerService.getActiveBannersResolved که همه را می‌دهد و کلاینت فیلتر می‌کند). */
    public List<ImageRowBanner> getAllActive() {
        return sortImagesWithin(repo.findByActiveTrueOrderByPositionAsc());
    }

    /** ترتیبِ عکس‌هایِ داخلِ هر پنجره را بر اساسِ ImageRowItem.position مرتب می‌کند —
     * مستقل از ترتیبِ افزودن/آپلود، طبقِ خواسته‌ی مالک («ایندکس‌گذاری روی عکس‌ها»). */
    private List<ImageRowBanner> sortImagesWithin(List<ImageRowBanner> groups) {
        for (ImageRowBanner g : groups) {
            if (g.getImages() != null) {
                g.getImages().sort(Comparator.comparingInt(ImageRowItem::getPosition));
            }
        }
        return groups;
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
        input.setColor(normalizeColor(input.getColor()));
        return repo.save(input);
    }

    /**
     * رنگ را به شکلِ یکدستِ {@code #rrggbb} درمی‌آورد؛ ورودیِ خالی/نامعتبر ←
     * {@code null} یعنی بی‌رنگ. سخت‌گیر است چون مقدارش بعداً مستقیم داخلِ
     * استایلِ HTML می‌نشیند و هر چیزِ دیگری راهِ تزریقِ CSS باز می‌کند.
     * (همان منطقِ CategoryService.normalizeColor — عمداً هم‌شکل.)
     */
    private String normalizeColor(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.isEmpty()) return null;
        if (!v.startsWith("#")) v = "#" + v;
        if (v.matches("(?i)^#[0-9a-f]{3}$")) {
            v = "#" + v.charAt(1) + v.charAt(1) + v.charAt(2) + v.charAt(2) + v.charAt(3) + v.charAt(3);
        }
        return v.matches("(?i)^#[0-9a-f]{6}$") ? v.toLowerCase() : null;
    }

    public void delete(String id) {
        if (!repo.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "پنجره یافت نشد");
        }
        repo.deleteById(id);
    }
}
