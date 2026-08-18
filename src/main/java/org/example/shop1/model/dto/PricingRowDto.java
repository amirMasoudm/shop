package org.example.shop1.model.dto;

import org.example.shop1.model.entity.Product;

import java.math.BigDecimal;

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
    private final String floorPriceCheckedAt;
    private final String floorPriceCheckedBy;

    private final Boolean pushSaleFlag;

    public PricingRowDto(Product p) {
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
        this.floorPriceCheckedAt = p.getFloorPriceCheckedAt() == null ? null : p.getFloorPriceCheckedAt().toString();
        this.floorPriceCheckedBy = p.getFloorPriceCheckedBy();
        this.pushSaleFlag = p.getPushSaleFlag();
    }

    public static PricingRowDto of(Product p) { return new PricingRowDto(p); }

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
    public String getFloorPriceCheckedAt() { return floorPriceCheckedAt; }
    public String getFloorPriceCheckedBy() { return floorPriceCheckedBy; }
    public Boolean getPushSaleFlag() { return pushSaleFlag; }
}
