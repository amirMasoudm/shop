package org.example.shop1.model.entity;

import org.example.shop1.model.enums.CourseMode;
import org.example.shop1.model.enums.OrderItemType;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

// این کلاس به عنوان یک کلاس داخلی (Embedded) در Order ذخیره می‌شود و Entity جدا نیست
public class OrderItem {

    // 🔴 اسنادِ قدیمی این فیلد را ندارند → پیش‌فرضِ PRODUCT (سازگاریِ عقب، طبقِ
    // docs/prompt-tech-chat-course-system.md). وقتی COURSE است، productId شناسه‌ی
    // دوره است، نه محصول — همان فیلد دوباره‌استفاده شده تا Order/OrderItem مجبور
    // به تغییرِ ساختار نشوند و یک سبدِ مختلط (کالا+دوره) طبیعی کار کند.
    private OrderItemType itemType = OrderItemType.PRODUCT;

    private String productId;
    private String productName;
    private String productImage; // برای نمایش در تاریخچه سفارشات بدون نیاز به جوین
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal unitPrice; // قیمت واحد در لحظه خرید
    private int quantity; // تعداد سفارش داده شده

    // فقط برایِ itemType=COURSE: حالتِ دوره در لحظه‌ی خرید (برایِ پنلِ ادمین، تا بداند
    // فیلدِ لایسنس را برایِ همین قلم نشان بدهد یا نه — دوره‌ی حضوری لایسنس ندارد)
    private CourseMode courseMode;

    // فقط برایِ دوره‌یِ آنلاین: ادمین بعدِ پرداختِ موفق، دستی روی همین سفارش وارد می‌کند
    private String licenseKey;
    private String downloadLink;

    public OrderItem() {}

    public OrderItem(String productId, String productName, String productImage, BigDecimal unitPrice, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.productImage = productImage;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public BigDecimal getTotalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    // Getters & Setters
    public OrderItemType getItemType() { return itemType; }
    public void setItemType(OrderItemType itemType) { this.itemType = itemType; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public CourseMode getCourseMode() { return courseMode; }
    public void setCourseMode(CourseMode courseMode) { this.courseMode = courseMode; }
    public String getLicenseKey() { return licenseKey; }
    public void setLicenseKey(String licenseKey) { this.licenseKey = licenseKey; }
    public String getDownloadLink() { return downloadLink; }
    public void setDownloadLink(String downloadLink) { this.downloadLink = downloadLink; }
}
