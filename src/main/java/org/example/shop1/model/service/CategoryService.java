package org.example.shop1.model.service;

import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.dto.CategoryOrderDto;
import org.example.shop1.model.dto.CategoryRequestDto;
import org.example.shop1.model.dto.CategoryResponseDto;
import org.example.shop1.model.entity.Category;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.reposritory.CategoryRepository;
import org.example.shop1.model.reposritory.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository repo;
    private final ProductRepository productRepo;

    public CategoryService(CategoryRepository repo, ProductRepository productRepo) {
        this.repo = repo;
        this.productRepo = productRepo;
    }

    // ایجاد دسته‌بندی با مدیریت تایپ (ONLINE/WAREHOUSE)
    public Category create(CategoryRequestDto dto) {
        Category category = new Category();
        category.setName(dto.getName());

        String typeToSave = dto.getType() != null && !dto.getType().isEmpty() ? dto.getType() : "ONLINE";
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

        // ترتیب: انتهای هم‌ردیف‌های همان والد قرار بگیرد
        category.setPosition(nextPositionAmongSiblings(typeToSave, category.getParentId()));

        return repo.save(category);
    }

    @Transactional
    public Category update(String id, CategoryRequestDto dto) {
        Category category = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (dto.getName() != null) {
            category.setName(dto.getName());
        }
        category.setFilterKeys(dto.getFilterKeys());
        if (dto.getType() != null && !dto.getType().isEmpty()) {
            category.setType(dto.getType());
        }
        // ابتدا نام/ویژگی‌ها ذخیره شود
        repo.save(category);

        // تغییر والد از فرم ویرایش: فیلد parentId (و newParentId برای سازگاری) پشتیبانی می‌شود
        String target = dto.getNewParentId() != null ? dto.getNewParentId() : dto.getParentId();
        if (target != null && target.trim().isEmpty()) target = null;

        String current = category.getParentId();
        boolean parentChanged = (target == null) ? (current != null) : !target.equals(current);
        if (parentChanged) {
            return moveCategory(id, target);
        }
        return repo.findById(id).orElse(category);
    }

    // شمارش محصولاتِ یک دسته و همه‌ی زیرمجموعه‌هایش (بر اساس نوع دسته)
    public long countProductsInSubtree(String id) {
        Category root = repo.findById(id).orElse(null);
        if (root == null) return 0;
        List<String> ids = subtreeCategoryIds(id);
        return isWarehouse(root)
                ? productRepo.countByWarehouseCategoryIdIn(ids)
                : productRepo.countByCategoryIdIn(ids);
    }

    /*
       حذف دسته با سه راهبرد برای تعیین تکلیف محصولات:
       - BLOCK   : اگر محصولی وجود دارد، حذف انجام نشود (پیش‌فرض)
       - REASSIGN: محصولات به دسته‌ی مقصد منتقل شوند، سپس دسته حذف شود
       - CASCADE : محصولات هم همراه دسته حذف شوند
    */
    @Transactional
    public void delete(String id, String mode, String targetCategoryId) {
        Category root = repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "دسته یافت نشد"));
        boolean warehouse = isWarehouse(root);

        List<Category> all = repo.findAll();
        Set<String> toDelete = new HashSet<>();
        findAllChildren(id, all, toDelete);
        List<String> catIds = new ArrayList<>(toDelete);

        List<Product> products = warehouse
                ? productRepo.findByWarehouseCategoryIdIn(catIds)
                : productRepo.findByCategoryIdIn(catIds);

        String m = (mode == null || mode.isBlank()) ? "BLOCK" : mode.trim().toUpperCase();

        switch (m) {
            case "REASSIGN":
                if (targetCategoryId == null || targetCategoryId.isBlank()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "دسته‌ی مقصد برای انتقال محصولات مشخص نشده است");
                }
                if (toDelete.contains(targetCategoryId)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "دسته‌ی مقصد نمی‌تواند دسته‌ی در حال حذف یا زیرمجموعه‌ی آن باشد");
                }
                repo.findById(targetCategoryId)
                        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "دسته‌ی مقصد یافت نشد"));
                for (Product p : products) {
                    if (warehouse) p.setWarehouseCategoryId(targetCategoryId);
                    else p.setCategoryId(targetCategoryId);
                }
                productRepo.saveAll(products);
                break;

            case "CASCADE":
                if (!products.isEmpty()) productRepo.deleteAll(products);
                break;

            case "BLOCK":
            default:
                if (!products.isEmpty()) {
                    throw new ApiException(HttpStatus.CONFLICT,
                            "این دسته " + products.size() + " محصول دارد؛ ابتدا تکلیف محصولات را مشخص کنید");
                }
                break;
        }

        repo.deleteAllById(toDelete);
    }

    // شناسه‌ی دسته + همه‌ی زیرمجموعه‌هایش
    private List<String> subtreeCategoryIds(String id) {
        List<Category> all = repo.findAll();
        Set<String> set = new HashSet<>();
        findAllChildren(id, all, set);
        return new ArrayList<>(set);
    }

    private boolean isWarehouse(Category c) {
        return c.getType() != null && "WAREHOUSE".equalsIgnoreCase(c.getType());
    }

    // متد دریافت درختواره با قابلیت فیلتر (مرتب‌شده بر اساس position)
    public List<CategoryResponseDto> getTree(String type) {
        List<Category> all;
        if (type != null && !type.trim().isEmpty()) {
            all = repo.findByType(type);
        } else {
            all = repo.findAll();
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

        sortByPosition(roots);
        return roots;
    }

    // مرتب‌سازی بازگشتی هر سطح بر اساس position
    private void sortByPosition(List<CategoryResponseDto> list) {
        list.sort(Comparator.comparingInt(d -> d.getPosition() == null ? 0 : d.getPosition()));
        for (CategoryResponseDto d : list) {
            sortByPosition(d.getChildren());
        }
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
        dto.setPosition(c.getPosition());
        dto.setFilterKeys(c.getFilterKeys());
        dto.setChildren(new ArrayList<>());
        return dto;
    }

    /*
       جابجایی یک دسته زیر والد جدید (استفاده در فرم ویرایش و endpoint /move).
       منطق تمیز: از حلقه جلوگیری می‌کند و ancestors/level خودش و همه فرزندان را بازحساب می‌کند.
    */
    @Transactional
    public Category moveCategory(String categoryId, String newParentId) {
        Category node = repo.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (newParentId != null && newParentId.trim().isEmpty()) newParentId = null;

        if (newParentId != null) {
            if (newParentId.equals(categoryId)) {
                throw new RuntimeException("نمی‌توانید یک دسته را زیرمجموعه خودش کنید!");
            }
            Category newParent = repo.findById(newParentId)
                    .orElseThrow(() -> new RuntimeException("New parent not found"));

            // جلوگیری از حلقه: والد جدید نباید یکی از زیرمجموعه‌های همین دسته باشد
            if (newParent.getAncestors() != null && newParent.getAncestors().contains(categoryId)) {
                throw new RuntimeException("نمی‌توانید دسته را زیر یکی از زیرمجموعه‌های خودش ببرید!");
            }

            node.setParentId(newParent.getId());
            List<String> anc = new ArrayList<>(newParent.getAncestors() != null ? newParent.getAncestors() : new ArrayList<>());
            anc.add(newParent.getId());
            node.setAncestors(anc);
            node.setLevel(anc.size());
        } else {
            node.setParentId(null);
            node.setAncestors(new ArrayList<>());
            node.setLevel(0);
        }

        node.setPosition(nextPositionAmongSiblings(node.getType(), node.getParentId()));
        repo.save(node);

        // بازحساب ancestors/level فرزندان
        updateChildrenLevel(node, repo.findAll());
        return node;
    }

    /*
       مرتب‌سازی/جابجایی کل درخت با یک درخواست (درگ‌دراپ).
       فرانت‌اند ساختار کامل درخت را می‌فرستد و اینجا parentId/position/ancestors/level
       همه به‌صورت قطعی از روی همان ساختار بازحساب می‌شود.
    */
    @Transactional
    public void reorder(List<CategoryOrderDto> items) {
        if (items == null || items.isEmpty()) return;

        Map<String, Category> byId = new HashMap<>();
        for (Category c : repo.findAll()) byId.put(c.getId(), c);

        // نقشه‌ی والدِ ارسال‌شده برای محاسبه‌ی ancestors
        Map<String, String> parentMap = new HashMap<>();
        for (CategoryOrderDto it : items) {
            String pid = (it.getParentId() != null && it.getParentId().trim().isEmpty()) ? null : it.getParentId();
            parentMap.put(it.getId(), pid);
        }

        List<Category> toSave = new ArrayList<>();
        for (CategoryOrderDto it : items) {
            Category c = byId.get(it.getId());
            if (c == null) continue;

            String pid = parentMap.get(it.getId());
            c.setParentId(pid);
            c.setPosition(it.getPosition() != null ? it.getPosition() : 0);

            // ساخت ancestors با بالا رفتن در زنجیره‌ی والدها (با محافظ حلقه)
            List<String> ancestors = new ArrayList<>();
            String cur = pid;
            int guard = 0;
            while (cur != null && guard++ < 1000) {
                ancestors.add(0, cur);
                cur = parentMap.containsKey(cur)
                        ? parentMap.get(cur)
                        : (byId.get(cur) != null ? byId.get(cur).getParentId() : null);
            }
            c.setAncestors(ancestors);
            c.setLevel(ancestors.size());
            toSave.add(c);
        }

        repo.saveAll(toSave);
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
                updateChildrenLevel(c, all);
            }
        }
    }

    // بزرگ‌ترین position بین هم‌ردیف‌ها + ۱ (برای قرار گرفتن در انتها)
    private int nextPositionAmongSiblings(String type, String parentId) {
        List<Category> pool = (type != null && !type.isEmpty()) ? repo.findByType(type) : repo.findAll();
        int max = -1;
        for (Category c : pool) {
            if (Objects.equals(c.getParentId(), parentId)) {
                int p = c.getPosition() != null ? c.getPosition() : 0;
                if (p > max) max = p;
            }
        }
        return max + 1;
    }
}
