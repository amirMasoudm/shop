package org.example.shop1.model.service;

import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.dto.BannerDisplayDto;
import org.example.shop1.model.entity.Article;
import org.example.shop1.model.entity.Banner;
import org.example.shop1.model.entity.Category;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.enums.BannerLinkType;
import org.example.shop1.model.reposritory.ArticleRepository;
import org.example.shop1.model.reposritory.BannerRepository;
import org.example.shop1.model.reposritory.CategoryRepository;
import org.example.shop1.model.reposritory.ProductRepository;
import org.example.shop1.model.service.util.ProductUrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BannerService {

    private static final Logger log = LoggerFactory.getLogger(BannerService.class);

    private final BannerRepository bannerRepo;
    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final ArticleRepository articleRepo;

    public BannerService(BannerRepository bannerRepo, ProductRepository productRepo,
                         CategoryRepository categoryRepo, ArticleRepository articleRepo) {
        this.bannerRepo = bannerRepo;
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.articleRepo = articleRepo;
    }

    public List<Banner> getAllForAdmin() {
        return bannerRepo.findAll();
    }

    public Banner save(Banner banner) {
        if (banner.getImageUrl() == null || banner.getImageUrl().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "عکسِ بنر الزامی است");
        }
        if (banner.getAltText() == null || banner.getAltText().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "متنِ جایگزینِ عکس (alt) الزامی است");
        }
        if (banner.getLinkType() == null) {
            banner.setLinkType(BannerLinkType.NONE);
        }
        if (banner.getLinkType() != BannerLinkType.NONE
                && (banner.getLinkTarget() == null || banner.getLinkTarget().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "برایِ این نوعِ لینک، مقصد را انتخاب/وارد کنید");
        }
        return bannerRepo.save(banner);
    }

    public void delete(String id) {
        bannerRepo.deleteById(id);
    }

    /**
     * بنرهایِ فعال، آماده‌ی رندر — هر بار از رویِ شناسه resolve می‌شود، نه از رویِ
     * آدرسِ ذخیره‌شده (که اصلاً وجود ندارد). بنری که هدفش (محصول/دسته/مقاله) پاک شده،
     * به‌جایِ لینکِ ۴۰۴، بی‌لینک نمایش داده می‌شود — نه حذف از اسلایدر.
     */
    public List<BannerDisplayDto> getActiveBannersResolved() {
        List<Banner> banners = bannerRepo.findByIsActiveOrderByOrderIndexAsc(true);
        List<BannerDisplayDto> result = new ArrayList<>();
        for (Banner b : banners) {
            String mobileImg = (b.getImageUrlMobile() != null && !b.getImageUrlMobile().isBlank())
                    ? b.getImageUrlMobile() : b.getImageUrl();
            String resolvedUrl = resolveUrl(b);
            boolean external = b.getLinkType() == BannerLinkType.EXTERNAL_URL && resolvedUrl != null;
            result.add(new BannerDisplayDto(b.getId(), b.getImageUrl(), mobileImg, b.getAltText(),
                    resolvedUrl, external));
        }
        return result;
    }

    private String resolveUrl(Banner b) {
        BannerLinkType type = b.getLinkType();
        String target = b.getLinkTarget();
        if (type == null || type == BannerLinkType.NONE || target == null || target.isBlank()) {
            return null;
        }

        try {
            switch (type) {
                case PRODUCT -> {
                    Optional<Product> p = productRepo.findById(target);
                    if (p.isEmpty()) {
                        log.warn("بنر به محصولِ حذف‌شده اشاره می‌کند (id={}) — بدونِ لینک رندر می‌شود", target);
                        return null;
                    }
                    return ProductUrlUtil.hybridPathEncoded(p.get());
                }
                case CATEGORY -> {
                    Optional<Category> c = categoryRepo.findById(target);
                    if (c.isEmpty()) {
                        log.warn("بنر به دسته‌ی حذف‌شده اشاره می‌کند (id={}) — بدونِ لینک رندر می‌شود", target);
                        return null;
                    }
                    String resolver = (c.get().getSlug() != null && !c.get().getSlug().isEmpty())
                            ? c.get().getSlug() : c.get().getId();
                    return "/category/" + UriUtils.encodePathSegment(resolver, StandardCharsets.UTF_8);
                }
                case ARTICLE -> {
                    Optional<Article> a = articleRepo.findById(target);
                    if (a.isEmpty()) {
                        log.warn("بنر به مقاله‌ی حذف‌شده اشاره می‌کند (id={}) — بدونِ لینک رندر می‌شود", target);
                        return null;
                    }
                    String resolver = (a.get().getSlug() != null && !a.get().getSlug().isEmpty())
                            ? a.get().getSlug() : a.get().getId();
                    return "/blog/" + UriUtils.encodePathSegment(resolver, StandardCharsets.UTF_8);
                }
                case INTERNAL_PATH -> {
                    return target.startsWith("/") ? target : "/" + target;
                }
                case EXTERNAL_URL -> {
                    return target;
                }
                default -> {
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("خطا در resolveکردنِ لینکِ بنر (type={}, target={}): {}", type, target, e.toString());
            return null;
        }
    }
}
