package org.example.shop1.model.service;

import org.example.shop1.model.dto.ProductRequest;
import org.example.shop1.model.entity.ActivityLog;
import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.Category;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.reposritory.CategoryRepository;
import org.example.shop1.model.reposritory.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final StockNotificationService stockNotificationService;

    private final ActivityLogService activityLog;

    /**
     * دستهٔ سایت — اجباری، و با خطای روشن.
     * <p>
     * 🔴 پیش از این {@code findById(null)} صدا زده می‌شد و مونگو
     * {@code IllegalArgumentException} می‌داد، یعنی کاربر به‌جای «دسته را انتخاب کن»
     * یک ۵۰۰ِ خام می‌دید. برای کارشناسی که از پنلِ فروش محصول می‌سازد، فرقِ این دو
     * فرقِ «می‌دانم چه کار کنم» با «سیستم خراب است» است.
     */
    private Category requireCategory(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "دستهٔ سایت انتخاب نشده است");
        }
        return categoryRepo.findById(categoryId)
                .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                        "دستهٔ سایت پیدا نشد: " + categoryId));
    }

    public ProductService(ProductRepository productRepo, CategoryRepository categoryRepo,
                          StockNotificationService stockNotificationService,
                          ActivityLogService activityLog) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.stockNotificationService = stockNotificationService;
        this.activityLog = activityLog;
    }

    /**
     * فیلدهایی که تغییرشان در تبِ محصولات ارزشِ لاگ‌شدن دارد.
     * <p>
     * عمداً همه‌ی فیلدها نیست: توضیحات و سئو و مشخصاتِ فنی مدام عوض می‌شوند و لاگ را
     * پر می‌کنند بدونِ اینکه به سؤالِ واقعی («چه کسی قیمت/موجودی/دسته را عوض کرد؟»)
     * جواب بدهند.
     */
    private record LoggedField(String label, java.util.function.Function<Product, Object> reader) {}

    private static final java.util.List<LoggedField> LOGGED_FIELDS = java.util.List.of(
            new LoggedField("name", Product::getName),
            new LoggedField("onlinePrice", Product::getOnlinePrice),
            new LoggedField("price", Product::getPrice),
            new LoggedField("basePrice", Product::getBasePrice),
            new LoggedField("stock", Product::getStock),
            new LoggedField("discountPercent", Product::getDiscountPercent),
            new LoggedField("unit", Product::getUnit),
            new LoggedField("slug", Product::getSlug),
            new LoggedField("categoryId", Product::getCategoryId));

    /**
     * عکسِ فیلدهایِ لاگ‌شدنی <b>پیش از</b> تغییر.
     * <p>
     * لازم است چون updateProduct همان انتیتیِ لودشده را در جا تغییر می‌دهد؛ اگر بعد
     * از تغییر مقایسه می‌کردیم، «قبل» و «بعد» یکی بودند و هیچ تغییری لاگ نمی‌شد.
     */
    private java.util.Map<String, Object> snapshot(Product product) {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        for (LoggedField f : LOGGED_FIELDS) {
            values.put(f.label(), f.reader().apply(product));
        }
        return values;
    }

    /** یک ردیفِ لاگ به‌ازای هر فیلدی که واقعاً عوض شده. */
    private void logProductDiff(java.util.Map<String, Object> before, Product after) {
        for (LoggedField f : LOGGED_FIELDS) {
            Object oldValue = before.get(f.label());
            Object newValue = f.reader().apply(after);
            if (java.util.Objects.equals(String.valueOf(oldValue), String.valueOf(newValue))) continue;
            activityLog.recordProduct(ActivityLog.Action.PRODUCT_UPDATE, ActivityLog.Source.MANUAL,
                    after.getId(), after.getName(), f.label(), oldValue, newValue);
        }
    }

    // متد کمکی برای اعمال تخفیف
    private void applyDiscount(Product product, Integer discountPercent) {
        if (discountPercent != null && discountPercent > 0 && product.getPrice() != null) {
            BigDecimal price = product.getPrice();
            BigDecimal discountFactor = BigDecimal.valueOf(100 - discountPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            product.setDiscountedPrice(price.multiply(discountFactor).setScale(2, RoundingMode.HALF_UP));
            product.setDiscountPercent(discountPercent);
        } else {
            product.setDiscountedPrice(product.getPrice());
            product.setDiscountPercent(0);
        }
    }

    /**
     * موجودی و فیلدهایِ میزِ کارِ قیمت‌گذاری.
     * <p>
     * 🔴 <b>رفعِ باگِ «در راه»:</b> اگر موجودیِ تفکیکیِ شعب ارسال شود،
     * {@code stock} (موجودیِ فروش) <b>فقط</b> اصفهان + تهران می‌شود و «در راه»
     * در آن شمرده نمی‌شود. همگام‌سازیِ ۱۶ اوت {@code stock = B+F+G} را اعمال کرده بود،
     * یعنی کالایِ خریداری‌شده‌ی نرسیده «موجود» نشان داده می‌شد — و چون
     * {@code availability}ِ ترب از {@code stock > 0} می‌آید، همان کالا به کلِ بازار
     * «موجود» اعلام می‌شد. اگر شعبه‌ها ارسال نشوند، رفتارِ قبلی حفظ می‌شود تا
     * مسیرهایِ قدیمی (پنلِ ادمین) نشکنند.
     */
    private void applyStockAndPricingFields(Product product, ProductRequest request) {
        boolean hasBranchStock = request.getStockIsfahan() != null || request.getStockTehran() != null;

        if (hasBranchStock) {
            product.setStockIsfahan(request.getStockIsfahan());
            product.setStockTehran(request.getStockTehran());
            product.setStock(product.getSellableStock()); // «در راه» عمداً بیرون است
        } else if (request.getStock() != null) {
            product.setStock(request.getStock());
        } else if (product.getStock() == null) {
            // ⚠️ اینجا منشأِ باگِ دیتالاس بود: قبلاً بی‌قید setStock(null) می‌شد، پس هر
            // PUTِ ناقص موجودی را نال می‌کرد و PUTِ بعدی روی همان محصول با NPE می‌شکست.
            // حالا: PUTِ ناقص مقدارِ موجود را دست نمی‌زند، و محصولِ تازه به‌جایِ نال با
            // صفر ساخته می‌شود (صفر یعنی ناموجود — پیش‌فرضِ امن؛ نال هیچ معنایی ندارد).
            product.setStock(0);
        }

        if (request.getIncomingStock() != null) product.setIncomingStock(request.getIncomingStock());
        if (request.getPartnerUnitPrice() != null) product.setPartnerUnitPrice(request.getPartnerUnitPrice());
        if (request.getPartnerBulkPrice() != null) product.setPartnerBulkPrice(request.getPartnerBulkPrice());
        if (request.getDollarPrice() != null) product.setDollarPrice(request.getDollarPrice());
        if (request.getPushSaleFlag() != null) product.setPushSaleFlag(request.getPushSaleFlag());
        // ⚠️ همان الگویِ «فقط اگر ارسال شد»: تیکِ برداشته‌شده false می‌فرستد (نه null)،
        // پس خاموش‌کردن کار می‌کند؛ ولی ذخیره از فرمی که این فیلد را ندارد تیک را
        // نمی‌پراند — درسِ تسکِ ۷.
        if (request.getTorobEnabled() != null) product.setTorobEnabled(request.getTorobEnabled());
        if (request.getTorobFloorPrice() != null) product.setTorobFloorPrice(request.getTorobFloorPrice());
        if (request.getTorobUrl() != null) product.setTorobUrl(request.getTorobUrl());
        if (request.getDigikalaFloorPrice() != null) product.setDigikalaFloorPrice(request.getDigikalaFloorPrice());
        if (request.getDigikalaUrl() != null) product.setDigikalaUrl(request.getDigikalaUrl());
    }

    // متد ایجاد محصول - با کنترل یونیک بودن ID و پر کردن فیلدهای جدید
    public Product createProduct(ProductRequest request) {

        // ۱. کنترل شناسه محصول
        if (request.getId() != null && !request.getId().trim().isEmpty()) {
            if (productRepo.existsById(request.getId())) {
                throw new RuntimeException("Product ID '" + request.getId() + "' already exists. Please choose a unique ID.");
            }
        }

        // ۲. بررسی وجود دسته‌بندی
        Category category = requireCategory(request.getCategoryId());

        Product product = new Product();
        product.setId(request.getId()); // تنظیم ID (اگر null باشد، Mongo آن را تولید می‌کند)
        product.setName(request.getName());
        product.setCategoryId(category.getId());
        product.setPrice(request.getPrice());
        applyStockAndPricingFields(product, request);
        applyDiscontinuation(product, request);
        product.setDescription(request.getDescription());
        product.setImages(request.getImages());
        if (request.getImageAlts() != null) product.setImageAlts(request.getImageAlts()); // فقط اگر ارسال شد (بدون پاک‌کردنِ ناخواسته)
        product.setSpecifications(request.getSpecifications());
        product.setBasePrice(request.getBasePrice()); // مقداردهی قیمت پایه
        product.setUpdatedAt(Instant.now()); // زمان ساخت همان زمان بروزرسانی اولیه است
        // ۳. تنظیم فیلدهای سورتینگ اولیه
        product.setCreatedAt(Instant.now());
        product.setSalesCount(0L);
        product.setReviewCount(0L);
        product.setAverageRating(0.0);
        product.setWarehouseDescription(request.getWarehouseDescription());

        // ---> کدهای جدید سئو <---
        product.setSeoTitle(request.getSeoTitle());
        product.setSeoDescription(request.getSeoDescription());
        // اسلاگ تمیز و یکتا (اگر ورودی خالی بود از نام محصول ساخته می‌شود)
        String slugBase = (request.getSlug() == null || request.getSlug().trim().isEmpty())
                ? request.getName() : request.getSlug();
        product.setSlug(generateUniqueSlug(slugBase, null));
        // ---> پایان کدهای جدید سئو <---

        applyRichContent(product, request);

        product.setUnit(request.getUnit());
        product.setPackQuantity(request.getPackQuantity());

        product.setOnlinePrice(request.getOnlinePrice());


        // ۴. اعمال تخفیف و ذخیره
        applyDiscount(product, request.getDiscountPercent());
// داخل متد createProduct و updateProduct:
        product.setWeight(request.getWeight());
        product.setLength(request.getLength());
        product.setWidth(request.getWidth());
        product.setHeight(request.getHeight());

        product.setWarehouseCategoryId(request.getWarehouseCategoryId());
        Product created = productRepo.save(product);
        activityLog.recordProduct(ActivityLog.Action.PRODUCT_CREATE, ActivityLog.Source.MANUAL,
                created.getId(), created.getName(), null, null, created.getName());
        return created;
    }

    // متد به‌روزرسانی محصول
    public Product updateProduct(String id, ProductRequest request) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // **نکته:** در آپدیت، ID اصلی نباید تغییر کند.
        // تغییرات را فقط روی سایر فیلدها اعمال می‌کنیم.

        // مقدارِ قبلیِ موجودی را نگه‌دار تا گذارِ «ناموجود → موجود» را تشخیص دهیم (تریگرِ اطلاع‌رسانی)
        // ⚠️ stock از نوعِ Integer است: unboxingِ بی‌گارد روی رکوردِ قدیمیِ نال‌دار NPE می‌داد
        // و محصول را برای همیشه غیرقابلِ‌آپدیت می‌کرد. نالِ کهنه = «ناموجود» تفسیر می‌شود.
        int oldStock = product.getStock() != null ? product.getStock() : 0;
        java.util.Map<String, Object> before = snapshot(product);

        product.setName(request.getName());
        product.setPrice(request.getPrice());
        applyStockAndPricingFields(product, request);
        applyDiscontinuation(product, request);
        product.setDescription(request.getDescription());
        product.setUpdatedAt(Instant.now()); // ثبت زمان آپدیت
        product.setOnlinePrice(request.getOnlinePrice());

        // ⚠️ فیلدهایِ انبار و ابعاد: merge، نه replace.
        // این فیلدها را پنلِ انبار (AnbarMali) پر می‌کند، ولی پنلِ فروشگاه (Admin)
        // همیشه در بدنهٔ PUT نمی‌فرستدشان — و قبلاً بی‌قید بازنویسی می‌شدند، یعنی هر
        // آپدیتِ ساده‌ی فروشگاه کارِ انبار را نال می‌کرد. همان الگویِ
        // applyStockAndPricingFields (فیلدهایِ میزِ قیمت‌گذاری) اینجا هم اعمال شد.
        // معاوضه‌ی آگاهانه: پاک‌کردنِ عمدیِ این فیلدها با PUT دیگر ممکن نیست؛
        // در برابرِ از‌دست‌رفتنِ بی‌صدایِ دیتا، این هزینه‌ی درستی است.
        if (request.getBasePrice() != null) product.setBasePrice(request.getBasePrice());
        if (request.getWarehouseDescription() != null) product.setWarehouseDescription(request.getWarehouseDescription());
        if (request.getUnit() != null) product.setUnit(request.getUnit());
        if (request.getPackQuantity() != null) product.setPackQuantity(request.getPackQuantity());

        if (request.getWeight() != null) product.setWeight(request.getWeight());
        if (request.getLength() != null) product.setLength(request.getLength());
        if (request.getWidth() != null) product.setWidth(request.getWidth());
        if (request.getHeight() != null) product.setHeight(request.getHeight());

        // ⚠️ Objects.equals و نه equals: محصولی که از ورودِ دسته‌ایِ انبار آمده ممکن است
        // اصلاً دسته نداشته باشد، و دقیقاً همان محصول‌هایی‌اند که «تکمیل کارت» رویشان
        // اجرا می‌شود. با equalsِ ساده اولین تلاش برای کامل‌کردنشان NPE می‌داد.
        if (!java.util.Objects.equals(product.getCategoryId(), request.getCategoryId())) {
            product.setCategoryId(requireCategory(request.getCategoryId()).getId());
        }

        product.setImages(request.getImages());
        if (request.getImageAlts() != null) product.setImageAlts(request.getImageAlts()); // فقط اگر ارسال شد (بدون پاک‌کردنِ ناخواسته)
        product.setSpecifications(request.getSpecifications());

        // اعمال مجدد تخفیف با درصد جدید
        applyDiscount(product, request.getDiscountPercent());
        // merge (نه replace) — به همان دلیلِ بالا: دسته‌ی انبار را پنلِ فروشگاه نمی‌فرستد
        if (request.getWarehouseCategoryId() != null) product.setWarehouseCategoryId(request.getWarehouseCategoryId());

        // ---> کدهای جدید سئو <---
        product.setSeoTitle(request.getSeoTitle());
        product.setSeoDescription(request.getSeoDescription());
        String slugBase = (request.getSlug() == null || request.getSlug().trim().isEmpty())
                ? request.getName() : request.getSlug();
        product.setSlug(generateUniqueSlug(slugBase, product.getId()));
        // ---> پایان کدهای جدید سئو <---

        applyRichContent(product, request);

        Product saved = productRepo.save(product);

        // گذارِ «ناموجود → موجود»: مشترکینِ اطلاع‌رسانی پیامک بگیرند (async؛ ذخیره را کند/شکننده نمی‌کند)
        // ⚠️ نقطهٔ دومِ unboxing. به‌خاطرِ short-circuitِ && فقط وقتی ارزیابی می‌شد که
        // oldStock <= 0 بود، پس در سناریویِ «موجودی داشت و نال شد» بی‌صدا رد می‌شد و
        // نال در دیتابیس می‌نشست — دقیقاً چیزی که کشفش را سخت کرده بود.
        int newStock = saved.getStock() != null ? saved.getStock() : 0;
        if (oldStock <= 0 && newStock > 0) {
            stockNotificationService.notifyBackInStock(saved.getId(), saved.getName());
        }

        logProductDiff(before, saved);
        return saved;
    }

    // متد مخصوص پنل ادمین (بدون صفحه‌بندی)
    public List<Product> getAllProductsForAdmin() {
        return productRepo.findAll();
    }

    // دریافت یک محصول با اسلاگ یا شناسه (برای صفحه محصول بدون نیاز به لود کل لیست)
    public Optional<Product> findBySlugOrId(String slugOrId) {
        Optional<Product> bySlug = productRepo.findBySlug(slugOrId);
        if (bySlug.isPresent()) return bySlug;
        return productRepo.findById(slugOrId);
    }

    /*
       ساخت اسلاگ تمیز و یکتا:
       - فاصله‌ها به خط تیره تبدیل می‌شوند
       - کاراکترهای خاص (/ ? # % ...) حذف می‌شوند تا URL نشکند
       - حروف/ارقام فارسی حفظ می‌شوند
       - در صورت تکراری بودن، یک شماره به انتها اضافه می‌شود (کالای اشتباه نمایش داده نشود)
    */
    private String generateUniqueSlug(String base, String excludeId) {
        String slug = slugify(base);
        if (slug.isEmpty()) slug = "product";

        String candidate = slug;
        int i = 2;
        while (true) {
            Optional<Product> existing = productRepo.findBySlug(candidate);
            if (existing.isEmpty() || existing.get().getId().equals(excludeId)) {
                return candidate;
            }
            candidate = slug + "-" + i++;
        }
    }

    private String slugify(String s) {
        return org.example.shop1.model.service.util.SlugUtil.slugify(s);
    }

    /*
       جدول مشخصات فنی، پرسش‌های متداول و محصولات مرتبط (با پاکسازی متن‌ها).
       ردیف‌های خالی حذف می‌شوند.
    */
    /** سقفِ پرش هنگامِ بررسیِ حلقه — همان عددی که مسیرِ درخواست هم به کار می‌برد. */
    public static final int MAX_REPLACEMENT_HOPS = 5;

    /**
     * توقفِ تولید و جایگزین — با همان قاعدهٔ «فقط اگر ارسال شد».
     * <p>
     * 🔴 حلقه در لحظهٔ ذخیره گرفته می‌شود، نه در لحظهٔ درخواست. «الف جایگزینش ب، ب
     * جایگزینش الف» اگر ذخیره می‌شد، هر دو صفحه بی‌مقصد می‌ماندند؛ مسیرِ درخواست
     * سقفِ پرش دارد و نمی‌شکند، ولی کارشناس هم نمی‌فهمید چرا ریدایرکت کار نمی‌کند.
     */
    private void applyDiscontinuation(Product product, ProductRequest request) {
        if (request.getDiscontinued() != null) product.setDiscontinued(request.getDiscontinued());
        if (request.getDiscontinuedNoticeVisible() != null) {
            product.setDiscontinuedNoticeVisible(request.getDiscontinuedNoticeVisible());
        }
        if (request.getReplacementProductId() == null) return;

        String rep = request.getReplacementProductId().trim();
        if (rep.isEmpty()) {                       // «بدونِ جایگزین»
            product.setReplacementProductId(null);
            return;
        }
        if (rep.equals(product.getId())) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "محصول نمی‌تواند جایگزینِ خودش باشد");
        }
        if (!productRepo.existsById(rep)) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "محصولِ جایگزین پیدا نشد");
        }
        // زنجیره را دنبال کن؛ اگر به خودِ این محصول برگشت، حلقه است
        if (product.getId() != null) {
            String cursor = rep;
            for (int hop = 0; hop < MAX_REPLACEMENT_HOPS && cursor != null; hop++) {
                if (cursor.equals(product.getId())) {
                    throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                            "این انتخاب حلقه می‌سازد: جایگزینِ انتخاب‌شده، خودش به همین محصول برمی‌گردد");
                }
                Product next = productRepo.findById(cursor).orElse(null);
                cursor = (next != null && next.isProductionStopped()) ? next.getReplacementProductId() : null;
            }
        }
        product.setReplacementProductId(rep);
    }

    private void applyRichContent(Product product, ProductRequest request) {
        // دُمِ فارسیِ آدرسِ هیبرید (اختیاری؛ اگر خالی بماند هنگام رندر از نامِ محصول ساخته می‌شود)
        if (request.getPersianSlug() != null) {
            String pt = org.example.shop1.model.service.util.SlugUtil.sanitizeTail(request.getPersianSlug());
            product.setPersianSlug(pt.isEmpty() ? null : pt);
        }

        if (request.getTechSpecs() != null) {
            List<org.example.shop1.model.entity.TechSpecRow> rows = new java.util.ArrayList<>();
            for (org.example.shop1.model.entity.TechSpecRow r : request.getTechSpecs()) {
                if (r == null) continue;
                String k = org.example.shop1.config.SecurityUtils.clean(r.getKey());
                String v = org.example.shop1.config.SecurityUtils.clean(r.getValue());
                if (k == null || k.isBlank() || v == null || v.isBlank()) continue;
                rows.add(new org.example.shop1.model.entity.TechSpecRow(
                        org.example.shop1.config.SecurityUtils.clean(r.getGroup()), k.trim(), v.trim()));
            }
            product.setTechSpecs(rows);
        }

        if (request.getFaqs() != null) {
            List<org.example.shop1.model.entity.FaqItem> faqs = new java.util.ArrayList<>();
            for (org.example.shop1.model.entity.FaqItem f : request.getFaqs()) {
                if (f == null) continue;
                String q = org.example.shop1.config.SecurityUtils.clean(f.getQuestion());
                String a = org.example.shop1.config.SecurityUtils.clean(f.getAnswer());
                if (q == null || q.isBlank() || a == null || a.isBlank()) continue;
                faqs.add(new org.example.shop1.model.entity.FaqItem(q.trim(), a.trim()));
            }
            product.setFaqs(faqs);
        }

        if (request.getRelatedProductIds() != null) {
            List<String> ids = request.getRelatedProductIds().stream()
                    .filter(id -> id != null && !id.isBlank())
                    .filter(id -> !id.equals(product.getId())) // خودش نباشد
                    .distinct().limit(10).collect(Collectors.toList());
            product.setRelatedProductIds(ids);
        }
    }

    // متد اصلی برای فرانت‌اند (با قابلیت فیلتر، سورتینگ و صفحه‌بندی)
// در فایل ProductService.java متد را به این شکل اصلاح کنید:

    public Page<Product> getProductsForClient(
            String categoryId,
            String sectionId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sortBy,
            String sortDirection,
            int page,
            int size) {

        Sort sort = defineSort(sortBy, sortDirection);
        Pageable pageable = PageRequest.of(page, size, sort);

        // فیلتر جشنواره (لندینگ سکشن) — سمت سرور تا فرانت نیازی به لود همه محصولات نداشته باشد
        if (sectionId != null && !sectionId.trim().isEmpty()) {
            return productRepo.findBySectionIdsContaining(sectionId, pageable);
        }

        if (categoryId != null && !categoryId.trim().isEmpty()) {
            // ۱. پیدا کردن تمام دسته‌هایی که این دسته‌بندی والد یا جدّ آن‌هاست
            // از فیلد ancestors که در کدهای CategoryService شما پر می‌شود استفاده می‌کنیم
            List<Category> subCategories = categoryRepo.findAll().stream()
                    .filter(c -> c.getAncestors().contains(categoryId) || c.getId().equals(categoryId))
                    .collect(Collectors.toList());

            // ۲. استخراج تمام IDها
            List<String> allCategoryIds = subCategories.stream()
                    .map(Category::getId)
                    .collect(Collectors.toList());

            // ۳. فراخوانی متد جدید ریپازیتوری
            return productRepo.findByCategoryIdIn(allCategoryIds, pageable);
        }

        return productRepo.findAll(pageable);
    }

    // متد تعریف استاندارد سورتینگ
    private Sort defineSort(String sortBy, String sortDirection) {
        String sortField;
        Sort.Direction direction;

        switch (sortBy != null ? sortBy.toLowerCase() : "createdat") {
            case "salescount":
                sortField = "salesCount";
                direction = Sort.Direction.DESC; // پرفروش‌ترین: نزولی
                break;
            case "averagerating":
                sortField = "averageRating";
                direction = Sort.Direction.DESC; // محبوب‌ترین: نزولی
                break;
            case "price":
                // سورت بر اساس قیمت تخفیف‌خورده
                sortField = "discountedPrice";
                direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
                break;
            case "createdat":
            default:
                sortField = "createdAt";
                direction = Sort.Direction.DESC; // جدیدترین: نزولی
        }

        return Sort.by(direction, sortField);
    }

    public void deleteProduct(String id) {
        // نامِ محصول پیش از حذف خوانده می‌شود، وگرنه لاگ فقط یک شناسه می‌شد و
        // «چه چیزی حذف شد؟» بی‌جواب می‌ماند.
        String name = productRepo.findById(id).map(Product::getName).orElse(null);
        productRepo.deleteById(id);
        activityLog.recordProduct(ActivityLog.Action.PRODUCT_DELETE, ActivityLog.Source.MANUAL,
                id, name, null, name, null);
    }
}