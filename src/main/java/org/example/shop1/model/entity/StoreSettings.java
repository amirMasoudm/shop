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

    // Getters & Setters

    public BigDecimal getRfqThreshold() { return rfqThreshold; }
    public void setRfqThreshold(BigDecimal rfqThreshold) { this.rfqThreshold = rfqThreshold; }

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