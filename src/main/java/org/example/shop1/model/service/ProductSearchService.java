package org.example.shop1.model.service;

import org.bson.types.Decimal128;
import org.example.shop1.model.dto.FilterResponseDto;
import org.example.shop1.model.dto.ProductSearchRequestDto;
import org.example.shop1.model.dto.ProductSearchResponseDto;
import org.example.shop1.model.entity.Category;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.reposritory.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ProductSearchService {

    private final MongoTemplate mongoTemplate;
    private final CategoryRepository categoryRepo;

    public ProductSearchService(MongoTemplate mongoTemplate, CategoryRepository categoryRepo) {
        this.mongoTemplate = mongoTemplate;
        this.categoryRepo = categoryRepo;
    }

    public ProductSearchResponseDto searchAndFilter(ProductSearchRequestDto request) {

        // ۱. ساخت کوئری پایه بر اساس درخواست
        Query query = new Query();

        // فیلتر دسته‌بندی (شامل زیرمجموعه‌ها)
        if (request.getCategoryId() != null && !request.getCategoryId().isEmpty()) {
            List<Category> subCats = categoryRepo.findAll().stream()
                    .filter(c -> request.getCategoryId().equals(c.getId()) || c.getAncestors().contains(request.getCategoryId()))
                    .collect(Collectors.toList());
            List<String> catIds = subCats.stream().map(Category::getId).collect(Collectors.toList());
            query.addCriteria(Criteria.where("categoryId").in(catIds));
        }

        // فیلتر سرچ متنی (نام) — Pattern.quote چون نام‌هایِ محصولِ میکروتیک پر از کاراکترهایِ
        // متا-regex هستند (+ در CCR2116-12G-4S+ و ...)؛ بدونِ quote، PatternSyntaxException می‌دهد
        if (request.getSearchQuery() != null && !request.getSearchQuery().isEmpty()) {
            query.addCriteria(Criteria.where("name").regex(Pattern.compile(Pattern.quote(request.getSearchQuery()), Pattern.CASE_INSENSITIVE)));
        }

        // فیلتر قیمت
        //
        // ⚠️ تبدیلِ صریح به Decimal128، نه اتکا به تبدیلِ خودکارِ QueryMapper.
        // باگِ واقعی که در تست پیدا شد: وقتی gte و lte هر دو رویِ یک آبجکتِ Criteria
        // می‌آیند، Spring Data مقدار را به رشتهٔ خام برمی‌گرداند (نه Decimal128) و
        // فیلترِ بازه هیچ‌چیز مچ نمی‌کند — با gte یا lte به‌تنهایی درست کار می‌کرد،
        // فقط وقتی هر دو با هم بودند می‌شکست. با ساختِ صریحِ Decimal128 این حدسِ
        // داخلی و شکننده کاملاً دور زده می‌شود.
        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            Criteria priceCriteria = Criteria.where("onlinePrice");
            if (request.getMinPrice() != null) priceCriteria.gte(new Decimal128(request.getMinPrice()));
            if (request.getMaxPrice() != null) priceCriteria.lte(new Decimal128(request.getMaxPrice()));
            query.addCriteria(priceCriteria);
        }

        // فیلتر ویژگی‌های داینامیک (نقش اصلی)
        if (request.getSpecifications() != null && !request.getSpecifications().isEmpty()) {
            request.getSpecifications().forEach((key, values) -> {
                if (values != null && !values.isEmpty()) {
                    // جستجو در مپ: specifications.key مقدارش یکی از مقادیر ارسالی باشد
                    query.addCriteria(Criteria.where("specifications." + key).in(values));
                }
            });
        }

        // ۲. محاسبه فیلترهای موجود در سایدبار (Faceted Search)
        List<FilterResponseDto> availableFilters = calculateAvailableFilters(query, request.getCategoryId());

        // ۳. اعمال صفحه‌بندی و مرتب‌سازی روی محصولات
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if ("priceAsc".equals(request.getSortBy())) sort = Sort.by(Sort.Direction.ASC, "onlinePrice");
        if ("priceDesc".equals(request.getSortBy())) sort = Sort.by(Sort.Direction.DESC, "onlinePrice");

        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize(), sort);

        long totalElements = mongoTemplate.count(query, Product.class); // تعداد کل نتایج

        query.with(pageRequest);
        List<Product> products = mongoTemplate.find(query, Product.class); // دریافت صفحه فعلی

        Page<Product> productPage = new PageImpl<>(products, pageRequest, totalElements);

        return new ProductSearchResponseDto(productPage, availableFilters);
    }

    // متد استخراج مقادیر موجود برای فیلترها (تا مواردی که محصول ندارند نمایش داده نشوند)
    private List<FilterResponseDto> calculateAvailableFilters(Query baseQuery, String categoryId) {
        List<FilterResponseDto> filterFacets = new ArrayList<>();

        if (categoryId == null || categoryId.isEmpty()) return filterFacets;

        // پیدا کردن کلیدهای فیلتر مختص این دسته
        Category category = categoryRepo.findById(categoryId).orElse(null);
        if (category == null || category.getFilterKeys() == null || category.getFilterKeys().isEmpty()) {
            return filterFacets;
        }

        // برای هر کلید فیلتر، می‌گردیم ببینیم چه مقادیری در نتایج جستجوی فعلی وجود دارد
        for (String filterKey : category.getFilterKeys()) {
            String fieldPath = "specifications." + filterKey;

            // استخراج تمام مقادیر یونیک این فیلد در محصولات پیدا شده
            List<String> distinctValues = mongoTemplate.findDistinct(baseQuery, fieldPath, Product.class, String.class);

            if (distinctValues != null && !distinctValues.isEmpty()) {
                List<FilterResponseDto.FilterOption> options = new ArrayList<>();
                for (String val : distinctValues) {
                    // (اختیاری): اگر می‌خواهید تعداد (Count) هر فیلتر را هم در بیاورید، باید اینجا کوئری aggregate بزنید.
                    // برای سرعت بیشتر، فعلا فقط نام فیلتر را بدون count برمی‌گردانیم.
                    options.add(new FilterResponseDto.FilterOption(val, 0));
                }
                filterFacets.add(new FilterResponseDto(filterKey, options));
            }
        }

        return filterFacets;
    }
}