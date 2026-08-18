package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Document(collection = "store_settings")
public class StoreSettings {
    @Id
    private String id = "origin_location"; // همیشه ثابت
    private String state;
    private String city;
    private String address;
    private Double lat;
    private Double lng;

    // آستانه‌ی مبلغی فعال‌شدن استعلام پیش‌فاکتور (RFQ). null یا ۰ یعنی غیرفعال.
    private BigDecimal rfqThreshold;

    /**
     * ضریبِ قیمتِ سایت: {@code onlinePrice = partnerBulkPrice × sitePriceFactor}.
     * <p>
     * پیش‌فرض ۱.۰۸ = ۵٪ سودِ فروشنده + ۳٪ سودِ کارگزار.
     * عمداً اینجاست و در کد هاردکد نشده — قبلاً فقط داخلِ اسکریپتِ یک‌بارمصرفِ ایمپورت بود
     * و اگر در اپ هم تکرار می‌شد، دو منبعِ حقیقت می‌شد و دیر یا زود واگرا می‌شدند.
     * یک قراردادِ تجاری است، پس فقط ADMIN تغییرش می‌دهد نه PRICER.
     */
    private BigDecimal sitePriceFactor;

    /** ضریبِ پیش‌فرض وقتی هنوز در تنظیمات ست نشده است. */
    public static final BigDecimal DEFAULT_SITE_PRICE_FACTOR = new BigDecimal("1.08");

    // Getters & Setters

    public BigDecimal getRfqThreshold() { return rfqThreshold; }
    public void setRfqThreshold(BigDecimal rfqThreshold) { this.rfqThreshold = rfqThreshold; }

    public BigDecimal getSitePriceFactor() { return sitePriceFactor; }
    public void setSitePriceFactor(BigDecimal sitePriceFactor) { this.sitePriceFactor = sitePriceFactor; }

    /** ضریبِ مؤثر — اگر ست نشده یا نامعتبر بود، پیش‌فرض. */
    public BigDecimal effectiveSitePriceFactor() {
        return (sitePriceFactor != null && sitePriceFactor.compareTo(BigDecimal.ZERO) > 0)
                ? sitePriceFactor : DEFAULT_SITE_PRICE_FACTOR;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
}