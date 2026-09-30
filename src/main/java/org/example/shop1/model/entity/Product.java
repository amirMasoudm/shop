package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "products")
public class Product {

    @Id
    private String id; // شناسه محصول که می‌تواند توسط ادمین وارد شود و باید یونیک باشد
    private String name;
    private String description;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal price;
    private Integer stock;
    private String categoryId;

    // --- فیلدهای سورتینگ و نمایش در فرانت‌اند ---
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal discountedPrice; // قیمت پس از اعمال تخفیف
    private Integer discountPercent = 0; // درصد تخفیف (0 تا 100)
    private Double averageRating = 0.0; // میانگین امتیاز
    private Long reviewCount = 0L; // تعداد کل امتیازات داده شده
    private Long salesCount = 0L; // تعداد فروش موفق (برای سورت پرفروش‌ترین)
    private Instant createdAt = Instant.now(); // تاریخ و زمان ایجاد (برای سورت جدیدترین)
    // ------------------------------------------

    private List<String> images = new ArrayList<>();
    // altِ اختصاصیِ تصاویر — موازی و هم‌ترتیبِ images (اختیاری؛ محصولِ قدیمی ندارد → fallback به alt خودکار)
    private List<String> imageAlts = new ArrayList<>();
    private Map<String, String> specifications = new HashMap<>();


    // در کلاس Product این فیلد را اضافه کنید
    private String warehouseCategoryId; // دسته‌بندی مختص انبار/فروشگاه فیزیکی
    // اضافه کردن قیمت پایه (فی)
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal basePrice;

    // اضافه کردن زمان آخرین بروزرسانی
    private Instant updatedAt;
    // فیلد جدید برای توضیحات داخلی انبار
    private String warehouseDescription;

    // ... سایر فیلدها ...

    private String unit; // واحد سنجش (عدد، کیلوگرم، بسته و...)
    private Integer packQuantity; // تعداد در بسته (فقط اگر واحد "بسته" باشد پر می‌شود)
    // استیکرها / سکشن‌های متصل به این محصول برای نمایش در لندینگ
    private List<String> sectionIds = new ArrayList<>();

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal onlinePrice; // قیمت فروش سایت

    // در فایل Product.java این فیلدها را به بدنه کلاس اضافه کنید:

    private Double weight; // به گرم
    private Double length; // به سانتی‌متر
    private Double width;  // به سانتی‌متر
    private Double height; // به سانتی‌متر

    // ==========================================
    // فیلدهای جدید مربوط به SEO
    // ==========================================
    private String slug;
    // دُمِ فارسیِ آدرسِ هیبریدِ محصول (تزئینی/سئو)؛ چت ب پرش می‌کند. اگر خالی بود fallback به نامِ محصول.
    private String persianSlug;
    private String seoTitle;
    private String seoDescription;

    // جدول مشخصات فنی گروه‌بندی‌شده (جدا از specifications فیلترپذیر)
    private List<TechSpecRow> techSpecs = new ArrayList<>();

    // پرسش‌های متداول محصول (اسکیمای FAQPage از رویش ساخته می‌شود)
    private List<FaqItem> faqs = new ArrayList<>();

    // محصولات مکمل/مرتبط دستی (انتخاب ادمین)
    private List<String> relatedProductIds = new ArrayList<>();

    // ==========================================
    // میزِ کارِ قیمت‌گذاریِ داخلی (جایگزینِ گوگل‌شیتِ شرکت)
    // ⚠️ همه‌ی این‌ها داخلی‌اند و عمداً در PublicProductDto نیستند.
    // ==========================================

    /** «فروش به همکار تک» — ردهٔ قیمتِ تکی (ستونِ C شیت). */
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal partnerUnitPrice;

    /** «فروش تعدادی» — ردهٔ قیمتِ عمده (خریدِ چندتایی)، جدا از قیمتِ همکارِ تک. */
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal partnerBulkPrice;

    /**
     * قیمتِ سایت دستی ست شده و نباید از فرمول بازنویسی شود.
     * <p>
     * بدونِ این پرچم، همان کلاسِ مشکلِ «مشتق‌شده در برابرِ دستی» رخ می‌دهد: کارشناس
     * برایِ کالایی عمداً قیمتِ ویژه می‌گذارد، فردا کسی «فروش تعدادی» را عوض می‌کند و
     * قیمتِ ویژه <b>بی‌صدا</b> پاک می‌شود.
     */
    private Boolean priceOverride;

    /**
     * قیمتِ دستی، بعداً درصد هم خورده است — برایِ برچسبِ «دستی درصدی».
     * <p>
     * فقط وقتی معنی دارد که {@link #priceOverride} هم true باشد. هر ویرایشِ
     * دستیِ بعدی این را پاک می‌کند، چون آن عدد دیگر حاصلِ درصد نیست.
     */
    private Boolean pricePercentAdjusted;

    /** قیمتِ خرید/مرجع به دلار — برایِ رصدِ حساسیت به نرخِ ارز، مستقل از basePriceِ ریالی. */
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal dollarPrice;

    /** موجودیِ انبارِ اصفهان (ستونِ F). */
    private Integer stockIsfahan;

    /** موجودیِ انبارِ تهران (ستونِ G). */
    private Integer stockTehran;

    /**
     * «در راه» — خریداری‌شده ولی نرسیده (ستونِ B).
     * <p>
     * ⚠️ عمداً <b>در {@code stock} شمرده نمی‌شود</b>: همگام‌سازیِ ۱۶ اوت
     * {@code stock = B+F+G} را اعمال کرده بود و کالایِ نرسیده «موجود» نشان داده می‌شد؛
     * چون {@code availability}ِ ترب از {@code stock > 0} می‌آید، کالایِ نرسیده به کلِ
     * بازار «موجود» اعلام می‌شد. حالا موجودیِ فروش فقط اصفهان + تهران است.
     */
    private Integer incomingStock;

    /** «خیلی بفروشید» — در شیتِ اصلی فقط با رنگِ نارنجی کدگذاری شده بود. */
    private Boolean pushSaleFlag;

    /**
     * ترتیبِ دستیِ ردیف در میزِ کارِ قیمت‌گذاری (درگ‌دراپ).
     * <p>
     * {@code null} یعنی «هنوز جابه‌جا نشده»؛ چنین ردیف‌هایی بعد از ردیف‌های
     * ترتیب‌دار و بر اساسِ نام می‌آیند. پس محصولاتِ موجود بدونِ هیچ مهاجرتی سرِ
     * جای خودشان می‌مانند و ترتیب فقط جایی عوض می‌شود که کسی عمداً دست برده.
     * <p>
     * ⚠️ این ترتیب <b>مشترک</b> است نه شخصی — خواستهٔ صریحِ مالک؛ هر کارشناسی
     * جابه‌جا کند، بقیه هم همان را می‌بینند.
     */
    private Integer workspacePosition;

    /**
     * آیا این محصول در ترب نمایش داده شود؟
     * <p>
     * 🔴 <b>{@code null} یعنی «فعال».</b> این تنها معنیِ درست است و اتفاقی نیست:
     * محصولاتی که پیش از افزودنِ این فیلد ساخته شده‌اند اصلاً آن را ندارند، و اگر
     * غایب‌بودن «خاموش» خوانده می‌شد، کلِ کاتالوگ یک‌شبه از ترب محو می‌شد — بدونِ
     * خطا، بدونِ لاگ، و تا وقتی کسی ترب را باز نکند نامعلوم.
     * <p>
     * به همین دلیل نوعش {@code Boolean} است نه {@code boolean}، و هرجا خوانده
     * می‌شود باید صریح {@code null} چک شود.
     */
    private Boolean torobEnabled;

    /**
     * توقفِ تولید (دیسکانتینو) — اعلامِ کارشناس که این کالا دیگر عرضه نمی‌شود.
     * <p>
     * پیاده‌سازیِ چارچوبِ سئوی ناموجودی که پیش‌تر تصمیمش گرفته شد: قطعِ دائم
     * <b>با</b> جایگزین → ۳۰۱ به جایگزین؛ قطعِ دائم <b>بی</b> جایگزین → صفحه می‌ماند.
     * هیچ‌کدام ۴۰۴ نمی‌شود، چون صفحهٔ ایندکس‌شده سرمایهٔ محتوایی است.
     * <p>
     * {@code null} یعنی «در تولید» — محصولاتِ قدیمی این فیلد را ندارند. برای خواندن
     * از {@link #isProductionStopped()} استفاده کن، نه از خودِ فیلد.
     */
    private Boolean discontinued;

    /**
     * شناسهٔ محصولِ جایگزین. فقط وقتی توقفِ تولید روشن است معنی دارد.
     * <p>
     * خودِ جایگزین ممکن است بعدها متوقف شود، پس ارجاع می‌تواند زنجیره شود. دنبال‌کردنش
     * با سقفِ پرش در لحظهٔ درخواست انجام می‌شود، نه اینجا.
     */
    private String replacementProductId;

    /**
     * آیا توقفِ تولید — و انتقال به جایگزین — با یک پیامِ اضافه به بازدیدکننده گفته
     * شود؟ تصمیمِ کارشناس است: گاهی گفتنش کمک می‌کند («این مدل جایگزینِ آن است»)،
     * گاهی فقط حواس‌پرتی است. {@code null} یعنی خیر.
     */
    private Boolean discontinuedNoticeVisible;

    // کفِ قیمتِ رقبا (برای تصمیمِ قیمت‌گذاری) — هرگز عمومی نشود
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal torobFloorPrice;
    private String torobUrl;

    /**
     * متنِ جست‌وجویِ ترب برایِ همین محصول، دست‌نویسِ کارشناس.
     * <p>
     * نامِ کاملِ محصول برایِ جست‌وجو بد است (خریدار مدل را خلاصه می‌زند)؛
     * کارشناس یک بار عبارتِ درست را می‌نویسد و دفعهٔ بعد همان می‌آید.
     * <p>
     * {@code null} یعنی «از عنوانِ محصول بساز» — پس پاک‌کردنِ فیلد
     * برگشت به حالتِ خودکار است، نه جست‌وجویِ خالی.
     */
    private String torobQuery;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal digikalaFloorPrice;
    private String digikalaUrl;

    /**
     * شناسهٔ محصول در دیجی‌کالا (DKP) — بعد از تأییدِ انسان ذخیره می‌شود.
     * <p>
     * وجودش یعنی هویتِ کالا یک‌بار تأیید شده و از این به بعد به‌روزرسانیِ قیمت
     * یک‌کلیکی است. بدونِ آن نمی‌شود خودکار قیمت گرفت، چون جست‌وجویِ دیجی‌کالا
     * محصولاتِ نامرتبط هم برمی‌گرداند و «قیمتِ کالایِ اشتباه بدتر از نداشتنِ قیمت است».
     */
    private String digikalaDkp;

    /**
     * نامِ فروشنده‌ای که آخرین بار «باکسِ خرید»ِ دیجی‌کالا را داشت.
     * <p>
     * هدفش یک چیز است: وقتی کفِ قیمتِ بازار خودِ ماییم، کارشناس نباید فکر کند
     * دارد با یک رقیب رقابت می‌کند. مقایسهٔ نام سمتِ سرور انجام می‌شود
     * (نگاه کن به {@code FloorPriceService.OUR_DIGIKALA_SELLER}).
     * <p>
     * {@code null} یعنی هنوز واکشی نشده یا دیجی‌کالا فروشنده‌ای برنگرداند.
     */
    private String digikalaSellerTitle;

    /** آخرین باری که کفِ قیمتِ رقبا بررسی شد و توسطِ چه کسی. */
    private Instant floorPriceCheckedAt;
    private String floorPriceCheckedBy;

    /**
     * کدِ کالا برایِ همگام‌سازیِ موجودی با نرم‌افزارِ حسابداری (هلو) — مثلاً {@code DN-0042}.
     * <p>
     * 🔴 <b>چرا فیلدِ واقعی و نه کلیدی در {@code specifications}:</b> آن مپ آزاد است،
     * ایندکسِ یکتا نمی‌گیرد، و محتوایش به‌عنوانِ «مشخصاتِ فنی» به بیرون هم می‌رود.
     * تطبیقِ موجودی به کلیدی نیاز دارد که دیتابیس خودش یکتاییِ آن را تضمین کند،
     * وگرنه دو محصول با یک کد یعنی موجودیِ جابه‌جا نوشته‌شده.
     * <p>
     * ⚠️ فقط هنگامِ <b>ساخت</b> صادر می‌شود. ذخیرهٔ محصولِ موجود نه کدِ تازه می‌گیرد و
     * نه کدِ فعلی را عوض می‌کند — وگرنه هر ویرایش یک شماره می‌سوزاند و کدی که شرکت
     * در هلو تایپ کرده بی‌صاحب می‌ماند.
     */
    private String holooCode;

    /**
     * آخرین باری که ایمپورتِ هلو این محصول را نوشت.
     * <p>
     * در کنارِ {@link #stockTouchedManuallyAt} تنها راهِ تشخیصِ «انسان بعد از ایمپورتِ
     * قبلی دستی عوضش کرده» است — همان ردیف‌هایی که پیش‌نمایش باید جدا و برجسته
     * نشان بدهد تا ویرایشِ دستی بی‌صدا بازنویسی نشود.
     */
    private Instant stockImportedAt;

    /** آخرین ویرایشِ دستیِ موجودی (میزِ کار یا پنل). */
    private Instant stockTouchedManuallyAt;

    public String getHolooCode() { return holooCode; }
    public void setHolooCode(String holooCode) { this.holooCode = holooCode; }

    public Instant getStockImportedAt() { return stockImportedAt; }
    public void setStockImportedAt(Instant stockImportedAt) { this.stockImportedAt = stockImportedAt; }

    public Instant getStockTouchedManuallyAt() { return stockTouchedManuallyAt; }
    public void setStockTouchedManuallyAt(Instant v) { this.stockTouchedManuallyAt = v; }

    /**
     * آیا موجودی بعد از آخرین ایمپورت دستی عوض شده؟
     * <p>
     * محصولی که هرگز ایمپورت نشده ولی دستی ویرایش شده هم «دستکاری‌شده» حساب می‌شود:
     * اولین ایمپورت هم می‌تواند کارِ انسان را بازنویسی کند.
     */
    public boolean isStockManuallyTouchedSinceImport() {
        if (stockTouchedManuallyAt == null) return false;
        return stockImportedAt == null || stockTouchedManuallyAt.isAfter(stockImportedAt);
    }

    public BigDecimal getPartnerUnitPrice() { return partnerUnitPrice; }
    public void setPartnerUnitPrice(BigDecimal partnerUnitPrice) { this.partnerUnitPrice = partnerUnitPrice; }

    public BigDecimal getPartnerBulkPrice() { return partnerBulkPrice; }
    public void setPartnerBulkPrice(BigDecimal partnerBulkPrice) { this.partnerBulkPrice = partnerBulkPrice; }

    public Boolean getPricePercentAdjusted() { return pricePercentAdjusted; }
    public void setPricePercentAdjusted(Boolean v) { this.pricePercentAdjusted = v; }

    /**
     * «همکار تک» دستی ست شده — خواهرِ {@code priceOverride}.
     * <p>
     * بدونِ این، ضریبِ تازه هر بار عددی را که کارشناس آگاهانه گذاشته بازمی‌نوشت.
     * همان قاعده‌ای که قیمتِ سایت از اول داشت.
     */
    private Boolean partnerUnitOverride;

    public Boolean getPartnerUnitOverride() { return partnerUnitOverride; }
    public void setPartnerUnitOverride(Boolean v) { this.partnerUnitOverride = v; }

    public Boolean getPriceOverride() { return priceOverride; }
    public void setPriceOverride(Boolean priceOverride) { this.priceOverride = priceOverride; }

    public BigDecimal getDollarPrice() { return dollarPrice; }
    public void setDollarPrice(BigDecimal dollarPrice) { this.dollarPrice = dollarPrice; }

    public Integer getStockIsfahan() { return stockIsfahan; }
    public void setStockIsfahan(Integer stockIsfahan) { this.stockIsfahan = stockIsfahan; }

    public Integer getStockTehran() { return stockTehran; }
    public void setStockTehran(Integer stockTehran) { this.stockTehran = stockTehran; }

    public Integer getIncomingStock() { return incomingStock; }
    public void setIncomingStock(Integer incomingStock) { this.incomingStock = incomingStock; }

    public Integer getWorkspacePosition() { return workspacePosition; }
    public void setWorkspacePosition(Integer workspacePosition) { this.workspacePosition = workspacePosition; }

    public Boolean getPushSaleFlag() { return pushSaleFlag; }
    public void setPushSaleFlag(Boolean pushSaleFlag) { this.pushSaleFlag = pushSaleFlag; }

    public Boolean getTorobEnabled() { return torobEnabled; }
    public void setTorobEnabled(Boolean torobEnabled) { this.torobEnabled = torobEnabled; }

    /** تنها جایِ تفسیرِ «نبودنِ فیلد = فعال» — هرجا لازم شد از همین‌جا بخوان. */
    public boolean isTorobVisible() {
        return torobEnabled == null || torobEnabled;
    }

    public Boolean getDiscontinued() { return discontinued; }
    public void setDiscontinued(Boolean discontinued) { this.discontinued = discontinued; }

    public String getReplacementProductId() { return replacementProductId; }
    public void setReplacementProductId(String replacementProductId) { this.replacementProductId = replacementProductId; }

    public Boolean getDiscontinuedNoticeVisible() { return discontinuedNoticeVisible; }
    public void setDiscontinuedNoticeVisible(Boolean v) { this.discontinuedNoticeVisible = v; }

    /**
     * تنها جایِ تفسیرِ «نبودنِ فیلد = در تولید».
     * <p>
     * ⚠️ عمداً {@code isDiscontinued} نام نگرفت: کنارِ {@code getDiscontinued} هر دو به
     * یک خاصیتِ JSON نگاشت می‌شدند و جکسون بر سرِ اینکه کدام را بنویسد می‌شکست.
     */
    public boolean isProductionStopped() {
        return Boolean.TRUE.equals(discontinued);
    }

    /** پیام فقط وقتی معنی دارد که محصول واقعاً متوقف باشد. */
    public boolean isDiscontinuedNoticeShown() {
        return isProductionStopped() && Boolean.TRUE.equals(discontinuedNoticeVisible);
    }

    public BigDecimal getTorobFloorPrice() { return torobFloorPrice; }
    public void setTorobFloorPrice(BigDecimal torobFloorPrice) { this.torobFloorPrice = torobFloorPrice; }

    public String getTorobQuery() { return torobQuery; }
    public void setTorobQuery(String torobQuery) { this.torobQuery = torobQuery; }

    public String getTorobUrl() { return torobUrl; }
    public void setTorobUrl(String torobUrl) { this.torobUrl = torobUrl; }

    public BigDecimal getDigikalaFloorPrice() { return digikalaFloorPrice; }
    public void setDigikalaFloorPrice(BigDecimal digikalaFloorPrice) { this.digikalaFloorPrice = digikalaFloorPrice; }

    public String getDigikalaUrl() { return digikalaUrl; }
    public void setDigikalaUrl(String digikalaUrl) { this.digikalaUrl = digikalaUrl; }

    public String getDigikalaSellerTitle() { return digikalaSellerTitle; }
    public void setDigikalaSellerTitle(String digikalaSellerTitle) { this.digikalaSellerTitle = digikalaSellerTitle; }

    public String getDigikalaDkp() { return digikalaDkp; }
    public void setDigikalaDkp(String digikalaDkp) { this.digikalaDkp = digikalaDkp; }

    public Instant getFloorPriceCheckedAt() { return floorPriceCheckedAt; }
    public void setFloorPriceCheckedAt(Instant floorPriceCheckedAt) { this.floorPriceCheckedAt = floorPriceCheckedAt; }

    public String getFloorPriceCheckedBy() { return floorPriceCheckedBy; }
    public void setFloorPriceCheckedBy(String floorPriceCheckedBy) { this.floorPriceCheckedBy = floorPriceCheckedBy; }

    /**
     * موجودیِ قابلِ فروش = اصفهان + تهران (بدونِ «در راه»).
     * محاسبه‌شونده؛ منبعِ حقیقت برایِ ست‌کردنِ {@code stock} هنگامِ ذخیره.
     */
    @org.springframework.data.annotation.Transient
    public int getSellableStock() {
        return (stockIsfahan == null ? 0 : stockIsfahan) + (stockTehran == null ? 0 : stockTehran);
    }

    public List<TechSpecRow> getTechSpecs() { return techSpecs; }
    public void setTechSpecs(List<TechSpecRow> techSpecs) { this.techSpecs = techSpecs; }

    public List<FaqItem> getFaqs() { return faqs; }
    public void setFaqs(List<FaqItem> faqs) { this.faqs = faqs; }

    public List<String> getRelatedProductIds() { return relatedProductIds; }
    public void setRelatedProductIds(List<String> relatedProductIds) { this.relatedProductIds = relatedProductIds; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getPersianSlug() { return persianSlug; }
    public void setPersianSlug(String persianSlug) { this.persianSlug = persianSlug; }

    /**
     * دُمِ فارسیِ نهاییِ آدرس (sanitizeشده): persianSlug اگر پر باشد، وگرنه از نامِ محصول ساخته می‌شود.
     * محاسبه‌شونده است (در دیتابیس ذخیره نمی‌شود) ولی در JSON خروجی می‌آید تا SPA هم از آن استفاده کند.
     */
    @org.springframework.data.annotation.Transient
    public String getPersianTail() {
        String base = (persianSlug != null && !persianSlug.isBlank()) ? persianSlug : name;
        return org.example.shop1.model.service.util.SlugUtil.sanitizeTail(base);
    }

    public String getSeoTitle() { return seoTitle; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; }

    public String getSeoDescription() { return seoDescription; }
    public void setSeoDescription(String seoDescription) { this.seoDescription = seoDescription; }
    // ==========================================

    // گترها و سترها:
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public Double getLength() { return length; }
    public void setLength(Double length) { this.length = length; }

    public Double getWidth() { return width; }
    public void setWidth(Double width) { this.width = width; }

    public Double getHeight() { return height; }
    public void setHeight(Double height) { this.height = height; }

    public BigDecimal getOnlinePrice() { return onlinePrice; }
    public void setOnlinePrice(BigDecimal onlinePrice) { this.onlinePrice = onlinePrice; }

    public List<String> getSectionIds() { return sectionIds; }
    public void setSectionIds(List<String> sectionIds) { this.sectionIds = sectionIds; }
    // Getters & Setters
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Integer getPackQuantity() { return packQuantity; }
    public void setPackQuantity(Integer packQuantity) { this.packQuantity = packQuantity; }

    public String getWarehouseDescription() { return warehouseDescription; }
    public void setWarehouseDescription(String warehouseDescription) { this.warehouseDescription = warehouseDescription; }

    // --- Getters & Setters ---
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    // Getter و Setter
    public String getWarehouseCategoryId() { return warehouseCategoryId; }
    public void setWarehouseCategoryId(String warehouseCategoryId) { this.warehouseCategoryId = warehouseCategoryId; }


    public Product() {}

    // --- Getters & Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public List<String> getImageAlts() { return imageAlts; }
    public void setImageAlts(List<String> imageAlts) { this.imageAlts = imageAlts; }

    public Map<String, String> getSpecifications() { return specifications; }
    public void setSpecifications(Map<String, String> specifications) { this.specifications = specifications; }

    // Getters & Setters فیلدهای جدید
    public BigDecimal getDiscountedPrice() { return discountedPrice; }
    public void setDiscountedPrice(BigDecimal discountedPrice) { this.discountedPrice = discountedPrice; }

    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Long getReviewCount() { return reviewCount; }
    public void setReviewCount(Long reviewCount) { this.reviewCount = reviewCount; }

    public Long getSalesCount() { return salesCount; }
    public void setSalesCount(Long salesCount) { this.salesCount = salesCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}