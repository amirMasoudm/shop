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
import java.util.HashMap;
import java.util.Map;
import java.util.List;

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

        // ترتیب: اگر ادمین صریح داد همان اعمال شود، وگرنه مثلِ قبل به انتهای هم‌ردیف‌ها اضافه شود
        category.setStripPosition(dto.getStripPosition());
        category.setPosition(dto.getPosition() != null ? dto.getPosition()
                : nextPositionAmongSiblings(typeToSave, category.getParentId()));

        // ---> سئو: اسلاگ یکتا + عنوان/توضیح <---
        String slugBase = (dto.getSlug() == null || dto.getSlug().trim().isEmpty()) ? dto.getName() : dto.getSlug();
        category.setSlug(generateUniqueSlug(slugBase, null));
        category.setSeoTitle(dto.getSeoTitle());
        category.setColor(normalizeColor(dto.getColor()));
        category.setSeoDescription(dto.getSeoDescription());
        category.setIntroText(dto.getIntroText());
        category.setBrandName(blankToNull(dto.getBrandName()));

        return repo.save(category);
    }

    @Transactional
    /**
     * ویرایشِ دسته — <b>ادغامی، نه جایگزینی</b>.
     * <p>
     * برای هر فیلد سه حالت هست و فقط بدنهٔ درخواست تعیینش می‌کند (نگاه کن به
     * {@link CategoryRequestDto#has}):
     * <ul>
     *   <li>کلید نیامده ← مقدارِ قبلی دست نمی‌خورد</li>
     *   <li>کلید با {@code null} آمده ← پاک می‌شود</li>
     *   <li>کلید با مقدار آمده ← ست می‌شود</li>
     * </ul>
     * 🔴 پیش از این هر فیلدی که در بدنه نبود پاک می‌شد، و {@code parentId}ِ نیامده دسته
     * را به ریشه می‌برد — یعنی یک PUTِ فقط-نام هم سئو را می‌برد هم درخت را.
     * <p>
     * ⚠️ پاک‌شدنی ماندنِ {@code stripPosition}/{@code seoTitle}/{@code color} عمدی است:
     * خالی‌گذاشتنِ این‌ها در پنل یعنی «برش دار» (مثلاً تگ از نوارِ زیرِ جست‌وجو
     * برداشته شود). برای همین رفع «اگر null بود ننویس» نیست؛ آن این قابلیت را می‌کشت.
     * پنل همیشه همهٔ کلیدها را صریح می‌فرستد، پس رفتارِ پنل عوض نشده است.
     */
    public Category update(String id, CategoryRequestDto dto) {
        Category category = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // نام و نوع و ترتیبِ منو ساختاری‌اند و پاک‌شدنی نیستند: null همان «دست نزن» است
        if (dto.getName() != null) {
            category.setName(dto.getName());
        }
        if (dto.has("filterKeys")) {
            category.setFilterKeys(dto.getFilterKeys());
        }
        if (dto.getType() != null && !dto.getType().isEmpty()) {
            category.setType(dto.getType());
        }
        if (dto.getPosition() != null) {
            category.setPosition(dto.getPosition());
        }
        // ⚠️ برخلافِ position این یکی پاک‌شدنی است: خالی‌گذاشتنِ فیلد در پنل
        // (یعنی null ِ صریح) یعنی «برگرد به ترتیبِ منو».
        if (dto.has("stripPosition")) {
            category.setStripPosition(dto.getStripPosition());
        }

        // ---> سئو و رنگ: پاک‌شدنی، ولی فقط وقتی صریحاً خواسته شود <---
        if (dto.has("seoTitle")) category.setSeoTitle(dto.getSeoTitle());
        if (dto.has("color")) category.setColor(normalizeColor(dto.getColor()));
        if (dto.has("seoDescription")) category.setSeoDescription(dto.getSeoDescription());
        if (dto.has("introText")) category.setIntroText(dto.getIntroText());
        // رشتهٔ خالی یعنی «پاکش کن» و به null تبدیل می‌شود: اسکیما باید بتواند فرقِ
        // «برند ندارد» را با «برندش رشتهٔ تهی است» بفهمد.
        if (dto.has("brandName")) category.setBrandName(blankToNull(dto.getBrandName()));
        // پایداری URL: اگر ادمین صریحاً اسلاگ داد، همان اعمال می‌شود؛
        // اگر نداد و دسته هنوز اسلاگ ندارد، از نام ساخته می‌شود؛ در غیر این صورت اسلاگ قبلی حفظ می‌شود
        if (dto.getSlug() != null && !dto.getSlug().trim().isEmpty()) {
            category.setSlug(generateUniqueSlug(dto.getSlug(), id));
        } else if (category.getSlug() == null || category.getSlug().isEmpty()) {
            category.setSlug(generateUniqueSlug(category.getName(), id));
        }

        // ابتدا نام/ویژگی‌ها ذخیره شود
        repo.save(category);

        // تغییر والد از فرم ویرایش: فیلد parentId (و newParentId برای سازگاری) پشتیبانی می‌شود.
        // 🔴 کلیدِ نیامده یعنی «والد دست نخورد» — نه ریشه. فقط null ِ صریح یعنی ریشه.
        // newParentId اولویتش را نگه می‌دارد: اگر آمده بود (حتی null)، همان برنده است.
        final String target;
        if (dto.has("newParentId")) {
            target = blankToNull(dto.getNewParentId());
        } else if (dto.has("parentId")) {
            target = blankToNull(dto.getParentId());
        } else {
            return repo.findById(id).orElse(category);
        }

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

    /**
     * شمارشِ محصولاتِ زیردرختِ <b>همهٔ</b> دسته‌های یک نوع، در یک رفت‌وبرگشت.
     * <p>
     * ⚠️ چرا این هست و از {@link #countProductsInSubtree} حلقه نمی‌زنیم:
     * فرانت برایِ کاشی‌هایِ دستهٔ موبایل شمارِ همه را با هم می‌خواهد. حلقه روی
     * آن متد یعنی برایِ ۲۰ دسته، ۲۰ کوئریِ count به‌علاوهٔ ۲۰ بارگذاریِ درخت.
     * این‌جا محصولات یک بار خوانده می‌شوند و شمارش در حافظه بالا می‌رود.
     *
     * @return نگاشتِ شناسهٔ دسته ← تعدادِ محصولِ خودش و همهٔ زیردسته‌هایش
     */
    public Map<String, Long> productCountsBySubtree(String type) {
        boolean warehouse = "WAREHOUSE".equalsIgnoreCase(type);
        List<Category> all = repo.findAll().stream()
                .filter(c -> warehouse == isWarehouse(c))
                .collect(Collectors.toList());

        // شمارِ مستقیمِ هر دسته
        Map<String, Long> direct = new HashMap<>();
        for (Product p : productRepo.findAll()) {
            String cid = warehouse ? p.getWarehouseCategoryId() : p.getCategoryId();
            if (cid != null && !cid.isEmpty()) direct.merge(cid, 1L, Long::sum);
        }

        // شمارِ هر دسته = خودش + همهٔ نیاکانش آن را می‌گیرند. با ancestors یک
        // پیمایشِ ساده کافی است و نیازی به بازسازیِ درخت نیست.
        Map<String, Long> out = new HashMap<>();
        for (Category c : all) out.put(c.getId(), direct.getOrDefault(c.getId(), 0L));
        for (Category c : all) {
            long own = direct.getOrDefault(c.getId(), 0L);
            if (own == 0) continue;
            List<String> anc = c.getAncestors();
            if (anc == null) continue;
            for (String a : anc) if (out.containsKey(a)) out.merge(a, own, Long::sum);
        }
        return out;
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

    /**
     * رنگ را به شکلِ یکدستِ {@code #rrggbb} درمی‌آورد.
     * <p>
     * ورودیِ خالی یا نامعتبر ← {@code null} یعنی «بی‌رنگ». سخت‌گیری‌اش عمدی
     * است: این مقدار بعداً مستقیم داخلِ استایلِ HTML می‌نشیند، پس هر چیزی جز
     * یک کدِ هگزِ شش‌رقمی رد می‌شود تا راهی برایِ تزریقِ CSS باز نماند.
     * سفید (#ffffff) رنگِ معتبری است و با بی‌رنگ اشتباه نمی‌شود.
     */
    private String normalizeColor(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.isEmpty()) return null;
        if (!v.startsWith("#")) v = "#" + v;
        if (v.matches("(?i)^#[0-9a-f]{3}$")) {   // #abc → #aabbcc
            v = "#" + v.charAt(1) + v.charAt(1) + v.charAt(2) + v.charAt(2) + v.charAt(3) + v.charAt(3);
        }
        return v.matches("(?i)^#[0-9a-f]{6}$") ? v.toLowerCase() : null;
    }

    private CategoryResponseDto toDto(Category c) {
        CategoryResponseDto dto = new CategoryResponseDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setParentId(c.getParentId());
        dto.setType(c.getType());
        dto.setLevel(c.getLevel());
        dto.setPosition(c.getPosition());
        dto.setStripPosition(c.getStripPosition());
        dto.setFilterKeys(c.getFilterKeys());
        dto.setSlug(c.getSlug());
        dto.setSeoTitle(c.getSeoTitle());
        dto.setColor(c.getColor());
        dto.setSeoDescription(c.getSeoDescription());
        dto.setIntroText(c.getIntroText());
        dto.setBrandName(c.getBrandName());
        dto.setChildren(new ArrayList<>());
        return dto;
    }

    // اسلاگ یکتا برای دسته (در صورت تکرار، شماره اضافه می‌شود)
    private String generateUniqueSlug(String base, String excludeId) {
        String slug = org.example.shop1.model.service.util.SlugUtil.slugify(base);
        if (slug.isEmpty()) slug = "category";

        String candidate = slug;
        int i = 2;
        while (true) {
            Optional<Category> existing = repo.findBySlug(candidate);
            if (existing.isEmpty() || existing.get().getId().equals(excludeId)) {
                return candidate;
            }
            candidate = slug + "-" + i++;
        }
    }

    // پیدا کردن دسته با اسلاگ یا شناسه (برای مسیر /category/{slugOrId})
    public Optional<Category> findBySlugOrId(String slugOrId) {
        Optional<Category> bySlug = repo.findBySlug(slugOrId);
        if (bySlug.isPresent()) return bySlug;
        return repo.findById(slugOrId);
    }

    // محصولات یک دسته و همه زیرمجموعه‌هایش (برای بلوک سروری صفحه دسته)
    public List<Product> getProductsInSubtree(String categoryId, int limit) {
        List<String> ids = subtreeCategoryIds(categoryId);
        List<Product> products = productRepo.findByCategoryIdIn(ids);
        return products.size() > limit ? products.subList(0, limit) : products;
    }

    /*
       جابجایی یک دسته زیر والد جدید (استفاده در فرم ویرایش و endpoint /move).
       منطق تمیز: از حلقه جلوگیری می‌کند و ancestors/level خودش و همه فرزندان را بازحساب می‌کند.
    */
    @Transactional
    /** رشتهٔ خالی/فاصله همان null است — پنل «ریشه» را گاهی این‌طور می‌فرستد. */
    private static String blankToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s;
    }

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

    /**
     * مرتب‌سازی/جابجایی کل درخت با یک درخواست (درگ‌دراپ).
     * فرانت‌اند ساختار کامل درخت را می‌فرستد و اینجا parentId/position/ancestors/level
     * همه به‌صورت قطعی از روی همان ساختار بازحساب می‌شود.
     * <p>
     * ⚠️ {@code parentId}ِ هر آیتم سه‌حالته است، مثلِ {@link #update}: کلیدِ نیامده
     * یعنی «والدِ فعلی بماند»، {@code null}ِ صریح یعنی ریشه. پیش از این آیتمی که کلید
     * را نداشت بی‌صدا به ریشه می‌رفت.
     */
    @Transactional
    public void reorder(List<CategoryOrderDto> items) {
        if (items == null || items.isEmpty()) return;

        Map<String, Category> byId = new HashMap<>();
        for (Category c : repo.findAll()) byId.put(c.getId(), c);

        // نقشه‌ی والدِ ارسال‌شده برای محاسبه‌ی ancestors
        Map<String, String> parentMap = new HashMap<>();
        for (CategoryOrderDto it : items) {
            final String pid;
            if (it.hasParentId()) {
                pid = blankToNull(it.getParentId());
            } else {
                Category existing = byId.get(it.getId());
                if (existing == null) continue;          // همان رفتارِ حلقهٔ بعدی: ناموجود نادیده
                pid = existing.getParentId();
            }
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
