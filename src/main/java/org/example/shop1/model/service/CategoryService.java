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

    // ایجاد دسته‌بندی با مدیریت تایپ (ONLINE/WAREHOUSE)
    public Category create(CategoryRequestDto dto) {
        System.out.println("2. Service: create() called.");

        Category category = new Category();
        category.setName(dto.getName());

        String typeToSave = dto.getType() != null && !dto.getType().isEmpty() ? dto.getType() : "ONLINE";
        System.out.println("   -> Logic: Type determined to be saved: " + typeToSave);

        category.setType(typeToSave);
        category.setFilterKeys(dto.getFilterKeys());

        if (dto.getParentId() != null && !dto.getParentId().trim().isEmpty()) {
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

        Category saved = repo.save(category);
        System.out.println("3. DB: Saved category with ID: " + saved.getId() + " and Type: " + saved.getType());
        return saved;
    }
    @Transactional
    public Category update(String id, CategoryRequestDto dto) {
        Category category = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (dto.getName() != null) {
            category.setName(dto.getName());
            category.setFilterKeys(dto.getFilterKeys());
        }
        if (dto.getNewParentId() != null) moveCategory(id, dto.getNewParentId());

        return repo.save(category);
    }

    public void delete(String id) {
        List<Category> all = repo.findAll();
        Set<String> toDelete = new HashSet<>();
        findAllChildren(id, all, toDelete);
        repo.deleteAllById(toDelete);
    }

    // متد دریافت درختواره با قابلیت فیلتر
    public List<CategoryResponseDto> getTree(String type) {
        System.out.println("2. Service: getTree() called with type: " + type);
        List<Category> all;

        if (type != null && !type.trim().isEmpty()) {
            System.out.println("   -> Querying DB for findByType(" + type + ")");
            all = repo.findByType(type);
        } else {
            System.out.println("   -> Querying DB for findAll()");
            all = repo.findAll();
        }

        System.out.println("   -> DB returned " + all.size() + " records.");
        if(all.size() > 0) {
            System.out.println("   -> First record type: " + all.get(0).getType());
        }

        Map<String, CategoryResponseDto> map = all.stream()
                .map(this::toDto)
                .collect(Collectors.toMap(CategoryResponseDto::getId, c -> c));

        List<CategoryResponseDto> roots = new ArrayList<>();

        for (CategoryResponseDto dto : map.values()) {
            String parentId = dto.getParentId();
            boolean isRoot = parentId == null || parentId.trim().isEmpty() || !map.containsKey(parentId);

            if (isRoot) {
                roots.add(dto);
            } else {
                CategoryResponseDto parent = map.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(dto);
                }
            }
        }

        System.out.println("3. Service: Logic found " + roots.size() + " root nodes.");
        return roots;
    }
    private void findAllChildren(String parentId, List<Category> all, Set<String> toDelete) {
        toDelete.add(parentId);
        for (Category c : all) {
            if (parentId.equals(c.getParentId())) {
                findAllChildren(c.getId(), all, toDelete);
            }
        }
    }

    private CategoryResponseDto toDto(Category c) {
        CategoryResponseDto dto = new CategoryResponseDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setParentId(c.getParentId());
        dto.setLevel(c.getLevel());
        dto.setFilterKeys(c.getFilterKeys());

        dto.setChildren(new ArrayList<>());
        return dto;
    }

    @Transactional
    public Category moveCategory(String categoryId, String newParentId) {
        Category categoryToMove = repo.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // ۱. جلوگیری از انتقال دسته به داخل خودش
        if (newParentId != null && newParentId.equals(categoryId)) {
            throw new RuntimeException("نمی‌توانید یک دسته را زیرمجموعه خودش کنید!");
        }

        Category newParent = null;
        if (newParentId != null && !newParentId.isEmpty()) {
            newParent = repo.findById(newParentId)
                    .orElseThrow(() -> new RuntimeException("New parent not found"));

            // ۲. تشخیص اینکه آیا والدِ جدید، در واقع فرزندِ این دسته است؟ (جابجایی معکوس)
            boolean isDescendant = newParent.getAncestors() != null && newParent.getAncestors().contains(categoryId);

            if (isDescendant) {
                // منطق شکستن حلقه: والد جدید را می‌بریم جای والد قدیم می‌گذاریم
                String oldParentId = categoryToMove.getParentId();
                newParent.setParentId(oldParentId);

                if (oldParentId != null) {
                    Category oldParent = repo.findById(oldParentId)
                            .orElseThrow(() -> new RuntimeException("Old parent not found"));
                    newParent.setAncestors(new ArrayList<>(oldParent.getAncestors()));
                    newParent.getAncestors().add(oldParent.getId());
                    newParent.setLevel(oldParent.getLevel() + 1);
                } else {
                    newParent.setParentId(null);
                    newParent.setAncestors(new ArrayList<>());
                    newParent.setLevel(0);
                }

                // ذخیره والد جدید در جایگاه جدیدش
                repo.save(newParent);

                // آپدیت کردن زیرمجموعه‌های والد جدید (برای اینکه آدرس اجدادشان درست شود)
                List<Category> allCats = repo.findAll();
                updateChildrenLevel(newParent, allCats);
            }
        }

        // ۳. حالا با خیال راحت، دسته اصلی را زیرمجموعه والد جدید می‌کنیم
        if (newParent != null) {
            categoryToMove.setParentId(newParent.getId());
            categoryToMove.setAncestors(new ArrayList<>(newParent.getAncestors()));
            categoryToMove.getAncestors().add(newParent.getId());
            categoryToMove.setLevel(newParent.getLevel() + 1);
        } else {
            categoryToMove.setParentId(null);
            categoryToMove.setAncestors(new ArrayList<>());
            categoryToMove.setLevel(0);
        }

        repo.save(categoryToMove);

        // ۴. آپدیت کردن زیرمجموعه‌های دسته منتقل شده
        List<Category> allCategoriesFinal = repo.findAll();
        updateChildrenLevel(categoryToMove, allCategoriesFinal);

        return categoryToMove;
    }

    // متد بازگشتی برای آپدیت کردن فرزندان
    private void updateChildrenLevel(Category parent, List<Category> all) {
        if (all == null || parent == null) return;

        for (Category c : all) {
            if (parent.getId().equals(c.getParentId())) {
                c.setAncestors(new ArrayList<>(parent.getAncestors()));
                c.getAncestors().add(parent.getId());
                c.setLevel(parent.getLevel() + 1);
                repo.save(c);

                // فراخوانی بازگشتی برای فرزندانِ این فرزند
                updateChildrenLevel(c, all);
            }
        }
    }
}