package org.example.shop1.model.entity;

public class Address {
    private String recipientName;  // نام تحویل گیرنده
    private String recipientPhone; // شماره تماس
    private String fullAddress;    // متن آدرس
    private String postalCode;     // کد پستی
    private Double latitude;       // عرض جغرافیایی (از نقشه)
    private Double longitude;      // طول جغرافیایی (از نقشه)
    private String state;          // استان (جدید)
    private String city;           // شهر (جدید)

    public Address() {}

    // سازنده کلاس به‌روزرسانی شد
    public Address(String recipientName, String recipientPhone, String fullAddress, String postalCode, Double latitude, Double longitude, String state, String city) {
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.fullAddress = fullAddress;
        this.postalCode = postalCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.state = state;
        this.city = city;
    }

    // Getters & Setters جدید
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    // بقیه Getters & Setters قبلی بدون تغییر بمانند...
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }
    public String getFullAddress() { return fullAddress; }
    public void setFullAddress(String fullAddress) { this.fullAddress = fullAddress; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}