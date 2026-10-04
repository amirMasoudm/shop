package org.example.shop1.model.dto;

public class AiRequestDto {
    private String name;
    private String attributes; // مشخصات محصول
    private String additionalInfo; // اطلاعات تکمیلی (برند، نحوه استفاده، ...)
    private String userDescription; // توضیح کاربر، در صورت وجود اولویت دارد

    // getters / setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAttributes() { return attributes; }
    public void setAttributes(String attributes) { this.attributes = attributes; }

    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }

    public String getUserDescription() { return userDescription; }
    public void setUserDescription(String userDescription) { this.userDescription = userDescription; }
}
