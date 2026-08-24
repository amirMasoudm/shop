package org.example.shop1.model.dto;

/**
 * یک روشِ ارسالِ قابلِ‌انتخاب — بدونِ قیمت.
 * <p>
 * هزینهٔ واقعیِ ارسال («پس‌کرایه») در لحظهٔ تحویل توسطِ شرکتِ حمل‌ونقل از گیرنده
 * گرفته می‌شود، نه توسطِ سایت؛ پس اینجا دیگر فیلدِ قیمتی وجود ندارد.
 */
public class ShippingOption {
    private String method;
    private String title;
    private String estimatedTime;

    public ShippingOption() {}

    public ShippingOption(String method, String title, String estimatedTime) {
        this.method = method;
        this.title = title;
        this.estimatedTime = estimatedTime;
    }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getEstimatedTime() { return estimatedTime; }
    public void setEstimatedTime(String estimatedTime) { this.estimatedTime = estimatedTime; }
}
