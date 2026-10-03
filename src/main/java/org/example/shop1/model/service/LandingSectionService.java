package org.example.shop1.model.service;

import org.example.shop1.model.entity.Category;
import org.example.shop1.model.entity.LandingSection;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.reposritory.CategoryRepository;
import org.example.shop1.model.reposritory.LandingSectionRepository;
import org.example.shop1.model.reposritory.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LandingSectionService {

    private final LandingSectionRepository sectionRepo;
    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;

    public LandingSectionService(LandingSectionRepository sectionRepo, ProductRepository productRepo, CategoryRepository categoryRepo) {
        this.sectionRepo = sectionRepo;
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
    }

    // متد ادمین: ایجاد یا آپدیت سکشن
    @Transactional
    public LandingSection saveSection(LandingSection section) {
        // ۱. ذخیره اطلاعات خود سکشن
        LandingSection savedSection = sectionRepo.save(section);

        // ۲. پاک کردن این sectionId از تمام محصولاتی که قبلاً این استیکر را داشتند
        // (برای زمانی که ادمین محصولی را از سکشن حذف می‌کند)
        List<Product> oldProducts = productRepo.findBySectionIdsContaining(savedSection.getId());
        for (Product p : oldProducts) {
            p.getSectionIds().remove(savedSection.getId());
        }
        productRepo.saveAll(oldProducts);

        if (!savedSection.isActive()) {
            return savedSection; // اگر غیرفعال است، نیازی به اتصال به محصولات جدید نیست
        }

        // ۳. پیدا کردن تمام محصولاتی که باید در این سکشن باشند
        Set<Product> targetProducts = new HashSet<>();

        // الف) اضافه کردن محصولات بر اساس Category انتخاب شده (شامل زیرمجموعه‌ها)
        if (section.getSelectedCategoryIds() != null && !section.getSelectedCategoryIds().isEmpty()) {
            List<Category> allCategories = categoryRepo.findAll();
            Set<String> validCategoryIds = new HashSet<>();

            for (String catId : section.getSelectedCategoryIds()) {
                validCategoryIds.add(catId);
                // پیدا کردن زیردسته‌ها با استفاده از ancestors
                allCategories.stream()
                        .filter(c -> c.getAncestors().contains(catId))
                        .forEach(c -> validCategoryIds.add(c.getId()));
            }

            // گرفتن محصولات این دسته‌بندی‌ها (بدون صفحه‌بندی برای آپدیت یکباره)
            // نکته: نیاز به متد findByCategoryIdIn در ریپازیتوری دارید که قبلا دارید
            List<Product> catProducts = productRepo.findByCategoryIdIn(List.copyOf(validCategoryIds), org.springframework.data.domain.Pageable.unpaged()).getContent();
            targetProducts.addAll(catProducts);
        }

        // ب) اضافه کردن محصولاتی که مستقیماً و تکی انتخاب شده‌اند
        if (section.getSelectedProductIds() != null && !section.getSelectedProductIds().isEmpty()) {
            List<Product> specificProducts = productRepo.findByIdIn(section.getSelectedProductIds());
            targetProducts.addAll(specificProducts);
        }

        // ۴. اختصاص آیدی این سکشن به محصولات هدف
        for (Product p : targetProducts) {
            if (p.getSectionIds() == null) {
                p.setSectionIds(new ArrayList<>());
            }
            if (!p.getSectionIds().contains(savedSection.getId())) {
                p.getSectionIds().add(savedSection.getId());
            }
        }

        // ذخیره تغییرات محصولات در دیتابیس
        productRepo.saveAll(targetProducts);

        return savedSection;
    }

    // گرفتن لیست سکشن‌ها برای صفحه ادمین
    public List<LandingSection> getAllForAdmin() {
        return sectionRepo.findAll();
    }

    // گرفتن لیست سکشن‌های فعال برای فرانت‌اند (لندینگ پیج)
    public List<LandingSection> getActiveSectionsForLanding() {
        return sectionRepo.findByIsActiveOrderByOrderIndexAsc(true);
    }

    // حذف سکشن
    public void deleteSection(String id) {
        // ابتدا استیکر را از محصولات پاک می‌کنیم
        List<Product> products = productRepo.findBySectionIdsContaining(id);
        for (Product p : products) {
            p.getSectionIds().remove(id);
        }
        productRepo.saveAll(products);

        // سپس خود سکشن را پاک می‌کنیم
        sectionRepo.deleteById(id);
    }
}