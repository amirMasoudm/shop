package org.example.shop1.model.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.shop1.model.dto.CategoryOrderDto;
import org.example.shop1.model.dto.CategoryRequestDto;
import org.example.shop1.model.entity.Category;
import org.example.shop1.model.reposritory.CategoryRepository;
import org.example.shop1.model.reposritory.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * نگهبانِ ادغامیِ {@code PUT /api/categories/{id}} — همان کلاسِ باگِ تسکِ ۷ رویِ محصول.
 * <p>
 * سه قاعده که شکستنشان بی‌صداست و هزینه‌اش سئو و <b>درختِ دسته‌ها</b> است:
 * <pre>
 *   کلید نیامده          ← دست نخورد
 *   کلید با null آمده   ← پاک شود
 *   کلید با مقدار آمده  ← ست شود
 * </pre>
 * ⚠️ بدنه‌ها عمداً با یک {@link ObjectMapper}ِ واقعی از JSON ساخته می‌شوند، نه با
 * ستِرِ دستی: کلِ سازوکار روی این تکیه دارد که Jackson ستِرِ کلیدِ غایب را صدا
 * نمی‌زند. ساختنِ DTO با ستِر، همان چیزی را دور می‌زد که باید آزموده شود.
 */
class CategoryUpdateMergeTest {

    private final ObjectMapper json = new ObjectMapper();
    private final Map<String, Category> db = new LinkedHashMap<>();
    private CategoryService service;

    @BeforeEach
    void setUp() {
        CategoryRepository repo = mock(CategoryRepository.class);
        service = new CategoryService(repo, mock(ProductRepository.class));

        when(repo.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(db.get(inv.getArgument(0, String.class))));
        when(repo.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            db.put(c.getId(), c);
            return c;
        });
        when(repo.saveAll(any())).thenAnswer(inv -> {
            Iterable<Category> all = inv.getArgument(0);
            all.forEach(c -> db.put(c.getId(), c));
            return all;
        });
        when(repo.findAll()).thenAnswer(inv -> new ArrayList<>(db.values()));
        when(repo.findByType(anyString())).thenAnswer(inv -> db.values().stream()
                .filter(c -> inv.getArgument(0, String.class).equals(c.getType())).toList());
        when(repo.findBySlug(anyString())).thenAnswer(inv -> db.values().stream()
                .filter(c -> inv.getArgument(0, String.class).equals(c.getSlug())).findFirst());

        // دو ریشه و یک زیردسته با همهٔ فیلدهایِ پاک‌شدنیِ پر
        db.put("r1", node("r1", null, "ریشهٔ یک"));
        db.put("r2", node("r2", null, "ریشهٔ دو"));
        Category child = node("c", "r1", "زیردسته");
        child.setAncestors(new ArrayList<>(List.of("r1")));
        child.setLevel(1);
        child.setSeoTitle("عنوانِ سئو");
        child.setSeoDescription("توضیحِ سئو");
        child.setIntroText("متنِ معرفی");
        child.setColor("#112233");
        child.setFilterKeys(new ArrayList<>(List.of("port")));
        child.setStripPosition(3);
        db.put("c", child);
    }

    private static Category node(String id, String parentId, String name) {
        Category c = new Category();
        c.setId(id);
        c.setParentId(parentId);
        c.setName(name);
        c.setSlug(id + "-slug");
        c.setType("ONLINE");
        c.setAncestors(new ArrayList<>());
        c.setLevel(0);
        c.setPosition(0);
        return c;
    }

    private Category put(String body) throws Exception {
        return service.update("c", json.readValue(body, CategoryRequestDto.class));
    }

    // ==========================================================
    // سیاههٔ خودِ تسک
    // ==========================================================

    /** 🔴 رگرسیونِ اصلی: PUTِ فقط-نام نه سئو را می‌برد نه درخت را. */
    @Test
    void nameOnlyLeavesEverythingElseAlone() throws Exception {
        Category c = put("{\"name\":\"X\"}");

        assertEquals("X", c.getName());
        assertEquals("r1", c.getParentId(), "والد نباید به ریشه برود");
        assertEquals(List.of("r1"), c.getAncestors());
        assertEquals(1, c.getLevel());
        assertEquals("عنوانِ سئو", c.getSeoTitle());
        assertEquals("توضیحِ سئو", c.getSeoDescription());
        assertEquals("متنِ معرفی", c.getIntroText());
        assertEquals("#112233", c.getColor());
        assertEquals(List.of("port"), c.getFilterKeys());
        assertEquals(3, c.getStripPosition());
    }

    /** پاک‌شدنی ماندنِ تگِ نوارِ جست‌وجو — دلیلِ اینکه رفع «null ننویس» نیست. */
    @Test
    void explicitNullStripPositionClearsIt() throws Exception {
        Category c = put("{\"name\":\"X\",\"stripPosition\":null}");
        assertNull(c.getStripPosition());
        assertEquals("عنوانِ سئو", c.getSeoTitle(), "فقط همان یکی پاک شود");
    }

    @Test
    void explicitNullSeoTitleClearsIt() throws Exception {
        Category c = put("{\"name\":\"X\",\"seoTitle\":null}");
        assertNull(c.getSeoTitle());
        assertEquals(3, c.getStripPosition(), "فقط همان یکی پاک شود");
    }

    @Test
    void explicitNullParentMovesToRoot() throws Exception {
        Category c = put("{\"name\":\"X\",\"parentId\":null}");
        assertNull(c.getParentId());
        assertTrue(c.getAncestors().isEmpty());
        assertEquals(0, c.getLevel());
    }

    @Test
    void explicitParentMovesAndRecomputesAncestors() throws Exception {
        Category c = put("{\"name\":\"X\",\"parentId\":\"r2\"}");
        assertEquals("r2", c.getParentId());
        assertEquals(List.of("r2"), c.getAncestors());
        assertEquals(1, c.getLevel());
    }

    // ==========================================================
    // newParentId و فرمِ کاملِ پنل
    // ==========================================================

    /** newParentId اولویتش را نگه می‌دارد — حتی null ِ صریحش. */
    @Test
    void newParentIdKeepsItsPriority() throws Exception {
        assertEquals("r2", put("{\"parentId\":\"r1\",\"newParentId\":\"r2\"}").getParentId());
        assertNull(put("{\"parentId\":\"r2\",\"newParentId\":null}").getParentId(),
                "newParentIdِ صریحاً null یعنی ریشه، حتی اگر parentId چیزِ دیگری بگوید");
    }

    /** رشتهٔ خالی همان ریشه است — شکلی که پنل گاهی ریشه را می‌فرستد. */
    @Test
    void blankParentMeansRoot() throws Exception {
        assertNull(put("{\"parentId\":\"  \"}").getParentId());
    }

    /**
     * پنل همیشه همهٔ کلیدها را صریح می‌فرستد؛ رفتارش نباید عوض شده باشد. خالی‌ها
     * {@code null} می‌روند و باید مثلِ قبل پاک کنند.
     */
    @Test
    void panelFullFormBehavesAsBefore() throws Exception {
        Category c = put("{\"name\":\"X\",\"parentId\":\"r1\",\"type\":\"ONLINE\",\"position\":null,"
                + "\"stripPosition\":null,\"filterKeys\":[],\"slug\":null,\"seoTitle\":null,"
                + "\"seoDescription\":null,\"introText\":null,\"color\":null}");
        assertEquals("r1", c.getParentId());
        assertEquals(List.of("r1"), c.getAncestors());
        assertNull(c.getSeoTitle());
        assertNull(c.getColor());
        assertNull(c.getStripPosition());
        assertEquals(List.of(), c.getFilterKeys());
        assertEquals("c-slug", c.getSlug(), "اسلاگِ خالی یعنی اسلاگِ قبلی بماند");
    }

    /**
     * 🔴 نمونهٔ واقعیِ همین باگ در پنلِ امروز: فرمِ دسته‌بندیِ دوره کلیدِ {@code color}
     * را اصلاً نمی‌فرستد، پس هر ذخیره‌اش رنگِ دسته را پاک می‌کرد.
     */
    @Test
    void courseFormWithoutColorKeepsTheColor() throws Exception {
        Category c = put("{\"name\":\"X\",\"parentId\":\"r1\",\"type\":\"ONLINE\",\"position\":null,"
                + "\"stripPosition\":3,\"filterKeys\":[\"port\"],\"slug\":null,\"seoTitle\":\"عنوانِ سئو\","
                + "\"seoDescription\":\"توضیحِ سئو\",\"introText\":\"متنِ معرفی\"}");
        assertEquals("#112233", c.getColor());
    }

    // ==========================================================
    // خودِ سازوکار
    // ==========================================================

    /** نامِ ثبت‌شده در هر ستِر باید دقیقاً همان کلیدِ JSON باشد؛ یک غلطِ تایپی یعنی فیلدِ مرده. */
    @Test
    void everyKeyIsTrackedUnderItsJsonName() throws Exception {
        String[] keys = {"name", "parentId", "newParentId", "type", "position", "stripPosition",
                "filterKeys", "slug", "seoTitle", "seoDescription", "introText", "color"};
        StringBuilder all = new StringBuilder("{");
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) all.append(',');
            all.append('"').append(keys[i]).append("\":null");
        }
        all.append('}');

        CategoryRequestDto full = json.readValue(all.toString(), CategoryRequestDto.class);
        CategoryRequestDto empty = json.readValue("{}", CategoryRequestDto.class);
        for (String k : keys) {
            assertTrue(full.has(k), "کلیدِ آمده باید ثبت شود: " + k);
            assertFalse(empty.has(k), "کلیدِ نیامده نباید ثبت شود: " + k);
        }
    }

    /** کلاینت نباید بتواند خودش تعیین کند چه چیزی «آمده» حساب شود. */
    @Test
    void clientCannotForgePresence() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper lenient = new com.fasterxml.jackson.databind.ObjectMapper()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        CategoryRequestDto dto = lenient.readValue("{\"present\":[\"seoTitle\"]}", CategoryRequestDto.class);
        assertFalse(dto.has("seoTitle"));
    }

    // ==========================================================
    // reorder
    // ==========================================================

    /** همان تله در درگ‌دراپ: آیتمِ بی‌کلید دیگر بی‌صدا به ریشه نمی‌رود. */
    @Test
    void reorderItemWithoutParentKeyKeepsItsParent() throws Exception {
        List<CategoryOrderDto> items = List.of(
                json.readValue("{\"id\":\"c\",\"position\":5}", CategoryOrderDto.class));
        service.reorder(items);

        Category c = db.get("c");
        assertEquals("r1", c.getParentId());
        assertEquals(List.of("r1"), c.getAncestors());
        assertEquals(5, c.getPosition());
    }

    /** ریشه‌ها در پنل با null ِ صریح می‌آیند و باید مثلِ قبل ریشه شوند. */
    @Test
    void reorderExplicitNullParentStillMeansRoot() throws Exception {
        List<CategoryOrderDto> items = List.of(
                json.readValue("{\"id\":\"c\",\"parentId\":null,\"position\":0}", CategoryOrderDto.class));
        service.reorder(items);

        Category c = db.get("c");
        assertNull(c.getParentId());
        assertEquals(0, c.getLevel());
    }
}
