package org.example.shop1.model.dto;

import org.example.shop1.model.entity.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * یک ردیفِ میزِ کارِ قیمت‌گذاری — نمایِ داخلی (فقط برایِ ADMIN/PRICER/SALES).
 * <p>
 * جدا از {@link PublicProductDto} است و عمداً فیلدهایِ داخلی را <b>دارد</b>؛
 * محافظتش در سطحِ مسیرِ {@code /api/v1/pricing/**} در SecurityConfig انجام می‌شود.
 */
public class PricingRowDto {

    private final String id;
    private final String name;
    private final String categoryId;

    private final BigDecimal onlinePrice;      // قیمتِ سایت
    private final BigDecimal partnerUnitPrice; // فروش به همکارِ تک
    private final BigDecimal partnerBulkPrice; // فروش تعدادی (عمده)
    private final BigDecimal dollarPrice;      // قیمتِ خرید/مرجع به دلار
    private final BigDecimal basePrice;        // قیمتِ خرید (فقط نمایش)

    // موجودی‌ها — در فازِ ۱ فقط-خواندنی (منبع: ورودِ دسته‌ای، بعداً هلو)
    private final Integer stockIsfahan;
    private final Integer stockTehran;
    private final Integer incomingStock;
    private final int sellableStock;
    private final Integer stock;

    private final BigDecimal torobFloorPrice;
    private final String torobUrl;
    private final BigDecimal digikalaFloorPrice;
    private final String digikalaUrl;
    private final String digikalaDkp;   // هویتِ تأییدشده در دیجی‌کالا (خالی = هنوز وصل نشده)
    private final String digikalaSellerTitle;   // فروشندهٔ باکسِ خرید در آخرین واکشی
    /** باکسِ خرید دستِ خودِ ماست؟ کفِ بازار آن‌وقت رقیب نیست، خودِ ماییم. */
    private final boolean weOwnBuyBox;
    private final String floorPriceCheckedAt;
    private final String floorPriceCheckedBy;

    private final Boolean pushSaleFlag;

    /** ترتیبِ دستیِ ردیف؛ null یعنی هنوز جابه‌جا نشده. */
    private final Integer workspacePosition;

    /** قیمتِ سایت دستی ست شده و فرمول بازنویسی‌اش نمی‌کند. */
    private final Boolean priceOverride;

    /** قیمتِ دستی، بعداً درصد هم خورده — برچسبِ «دستی درصدی». */
    private final Boolean pricePercentAdjusted;

    /** مقدارِ پیشنهادیِ فرمول — تا کارشناس ببیند قیمتِ دستی چقدر از فرمول عقب افتاده. */
    private final BigDecimal suggestedOnlinePrice;

    /** آدرسِ اختصاصیِ محصول در فروشگاه؛ خالی یعنی فقط با شناسه باز می‌شود. */
    private final String slug;

    /**
     * چه چیزی برای «کارتِ فروشگاه» کم دارد — خالی یعنی کارتش کامل است.
     * <p>
     * 🔴 محصول همیشه با شناسه باز می‌شود، پس «کارت ندارد» یعنی <b>کارتِ درست‌وحسابی
     * ندارد</b>، نه اینکه صفحه‌اش ۴۰۴ بدهد. ملاک همان سه چیزی است که مشتری در کارت
     * می‌بیند: عکس، توضیحات، و دستهٔ سایت. محصولی که از ورودِ دسته‌ایِ انبار آمده
     * معمولاً هر سه را ندارد و در فروشگاه کارتِ خالی نشان می‌دهد.
     */
    private final List<String> cardMissing;

    public PricingRowDto(Product p) {
        this(p, null);
    }

    public PricingRowDto(Product p, BigDecimal suggestedOnlinePrice) {
        this.suggestedOnlinePrice = suggestedOnlinePrice;
        this.priceOverride = p.getPriceOverride();
        this.pricePercentAdjusted = p.getPricePercentAdjusted();
        this.id = p.getId();
        this.name = p.getName();
        this.categoryId = p.getCategoryId();
        this.onlinePrice = p.getOnlinePrice();
        this.partnerUnitPrice = p.getPartnerUnitPrice();
        this.partnerBulkPrice = p.getPartnerBulkPrice();
        this.dollarPrice = p.getDollarPrice();
        this.basePrice = p.getBasePrice();
        this.stockIsfahan = p.getStockIsfahan();
        this.stockTehran = p.getStockTehran();
        this.incomingStock = p.getIncomingStock();
        this.sellableStock = p.getSellableStock();
        this.stock = p.getStock();
        this.torobFloorPrice = p.getTorobFloorPrice();
        this.torobUrl = p.getTorobUrl();
        this.digikalaFloorPrice = p.getDigikalaFloorPrice();
        this.digikalaUrl = p.getDigikalaUrl();
        this.digikalaDkp = p.getDigikalaDkp();
        this.digikalaSellerTitle = p.getDigikalaSellerTitle();
        this.weOwnBuyBox = org.example.shop1.model.service.marketplace.FloorPriceService.weOwnBuyBox(p);
        this.floorPriceCheckedAt = p.getFloorPriceCheckedAt() == null ? null : p.getFloorPriceCheckedAt().toString();
        this.floorPriceCheckedBy = p.getFloorPriceCheckedBy();
        this.pushSaleFlag = p.getPushSaleFlag();
        this.workspacePosition = p.getWorkspacePosition();
        this.slug = p.getSlug();

        List<String> missing = new ArrayList<>(3);
        if (p.getImages() == null || p.getImages().isEmpty()) missing.add("عکس");
        if (isBlank(p.getDescription())) missing.add("توضیحات");
        if (isBlank(p.getCategoryId())) missing.add("دستهٔ سایت");
        this.cardMissing = List.copyOf(missing);
    }

    private static boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    public static PricingRowDto of(Product p) { return new PricingRowDto(p); }

    public static PricingRowDto of(Product p, BigDecimal suggestedOnlinePrice) {
        return new PricingRowDto(p, suggestedOnlinePrice);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategoryId() { return categoryId; }
    public BigDecimal getOnlinePrice() { return onlinePrice; }
    public BigDecimal getPartnerUnitPrice() { return partnerUnitPrice; }
    public BigDecimal getPartnerBulkPrice() { return partnerBulkPrice; }
    public BigDecimal getDollarPrice() { return dollarPrice; }
    public BigDecimal getBasePrice() { return basePrice; }
    public Integer getStockIsfahan() { return stockIsfahan; }
    public Integer getStockTehran() { return stockTehran; }
    public Integer getIncomingStock() { return incomingStock; }
    public int getSellableStock() { return sellableStock; }
    public Integer getStock() { return stock; }
    public BigDecimal getTorobFloorPrice() { return torobFloorPrice; }
    public String getTorobUrl() { return torobUrl; }
    public BigDecimal getDigikalaFloorPrice() { return digikalaFloorPrice; }
    public String getDigikalaUrl() { return digikalaUrl; }
    public String getDigikalaDkp() { return digikalaDkp; }
    public String getDigikalaSellerTitle() { return digikalaSellerTitle; }
    public boolean isWeOwnBuyBox() { return weOwnBuyBox; }
    public String getFloorPriceCheckedAt() { return floorPriceCheckedAt; }
    public String getFloorPriceCheckedBy() { return floorPriceCheckedBy; }
    public Boolean getPushSaleFlag() { return pushSaleFlag; }
    public Integer getWorkspacePosition() { return workspacePosition; }
    public Boolean getPriceOverride() { return priceOverride; }
    public Boolean getPricePercentAdjusted() { return pricePercentAdjusted; }
    public BigDecimal getSuggestedOnlinePrice() { return suggestedOnlinePrice; }
    public String getSlug() { return slug; }
    public List<String> getCardMissing() { return cardMissing; }
}
