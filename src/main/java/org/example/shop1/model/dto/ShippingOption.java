package org.example.shop1.model.dto;

public class ShippingOption {
    private String method;
    private String title;
    private Long cost;
    private String estimatedTime;

    public ShippingOption() {}

    public ShippingOption(String method, String title, Long cost, String estimatedTime) {
        this.method = method;
        this.title = title;
        this.cost = cost;
        this.estimatedTime = estimatedTime;
    }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Long getCost() { return cost; }
    public void setCost(Long cost) { this.cost = cost; }
    public String getEstimatedTime() { return estimatedTime; }
    public void setEstimatedTime(String estimatedTime) { this.estimatedTime = estimatedTime; }
}