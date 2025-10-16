package org.example.shop1.model.service;

import org.example.shop1.model.dto.CategoryRequestDto;
import org.example.shop1.model.dto.CategoryResponseDto;
import org.example.shop1.model.entity.Category;
import org.example.shop1.model.reposritory.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository repo;

    public CategoryService(CategoryRepository repo) {
        this.repo = repo;
    }

    public Category create(CategoryRequestDto dto) {
        Category category = new Category();
        category.setName(dto.getName());

        if (dto.getParentId() != null) {
            Category parent = repo.findById(dto.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent not found"));
            category.setParentId(parent.getId());
            category.setAncestors(new ArrayList<>(parent.getAncestors()));
            category.getAncestors().add(parent.getId());
            category.setLevel(parent.getLevel() + 1);
        } else {
            category.setParentId(null);
            category.setAncestors(Collections.emptyList());
            category.setLevel(0);
        }

        return repo.save(category);
    }

    @Transactional
    public Category update(String id, CategoryRequestDto dto) {
        Category category = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (dto.getName() != null)
            category.setName(dto.getName());

        if (dto.getNewParentId() != null)
            moveCategory(id, dto.getNewParentId());

        return repo.save(category);
    }

    public void delete(String id) {
        List<Category> all = repo.findAll();
        Set<String> toDelete = new HashSet<>();
        findAllChildren(id, all, toDelete);
        repo.deleteAllById(toDelete);
    }

    private void findAllChildren(String parentId, List<Category> all, Set<String> toDelete) {
        toDelete.add(parentId);
        for (Category c : all) {
            if (parentId.equals(c.getParentId())) {
                findAllChildren(c.getId(), all, toDelete);
            }
        }
    }

    public List<CategoryResponseDto> getTree() {
        List<Category> all = repo.findAll();

        Map<String, CategoryResponseDto> map = all.stream()
                .map(this::toDto)
                .collect(Collectors.toMap(CategoryResponseDto::getId, c -> c));

        List<CategoryResponseDto> roots = new ArrayList<>();

        for (CategoryResponseDto dto : map.values()) {
            if (dto.getParentId() == null) {
                roots.add(dto);
            } else {
                CategoryResponseDto parent = map.get(dto.getParentId());
                if (parent != null) parent.getChildren().add(dto);
            }
        }

        return roots;
    }

    private CategoryResponseDto toDto(Category c) {
        CategoryResponseDto dto = new CategoryResponseDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setParentId(c.getParentId());
        dto.setLevel(c.getLevel());
        dto.setChildren(new ArrayList<>());
        return dto;
    }

    @Transactional
    public Category moveCategory(String categoryId, String newParentId) {
        Category category = repo.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (newParentId != null && newParentId.equals(categoryId)) {
            throw new RuntimeException("نمی‌توان دسته را زیر خودش قرار داد");
        }

        Category newParent = null;
        if (newParentId != null) {
            newParent = repo.findById(newParentId)
                    .orElseThrow(() -> new RuntimeException("New parent not found"));

            // بررسی اینکه والد جدید زیرشاخه خودش نباشد
            List<Category> all = repo.findAll();
            Set<String> descendants = new HashSet<>();
            findAllChildren(categoryId, all, descendants);
            if (descendants.contains(newParentId)) {
                throw new RuntimeException("نمی‌توان دسته را زیر یکی از زیرشاخه‌هایش قرار داد");
            }
        }

        // تنظیم والد جدید و سطح
        if (newParent != null) {
            category.setParentId(newParent.getId());
            category.setAncestors(new ArrayList<>(newParent.getAncestors()));
            category.getAncestors().add(newParent.getId());
            category.setLevel(newParent.getLevel() + 1);
        } else {
            category.setParentId(null);
            category.setAncestors(Collections.emptyList());
            category.setLevel(0);
        }

        repo.save(category);

        // آپدیت سطح و مسیر تمام زیرشاخه‌ها
        List<Category> all = repo.findAll();
        updateChildrenLevel(category, all);

        return category;
    }

    private void updateChildrenLevel(Category parent, List<Category> all) {
        for (Category c : all) {
            if (parent.getId().equals(c.getParentId())) {
                c.setAncestors(new ArrayList<>(parent.getAncestors()));
                c.getAncestors().add(parent.getId());
                c.setLevel(parent.getLevel() + 1);
                repo.save(c);
                updateChildrenLevel(c, all);
            }
        }
    }
}
