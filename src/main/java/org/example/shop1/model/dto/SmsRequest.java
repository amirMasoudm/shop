package org.example.shop1.model.dto;

import java.util.List;

public class SmsRequest {
    private String mobile;
    private int templateId;
    private List<SmsParameter> parameters;

    public SmsRequest(String mobile, int templateId, List<SmsParameter> parameters) {
        this.mobile = mobile;
        this.templateId = templateId;
        this.parameters = parameters;
    }
    // Getters and Setters...
    public String getMobile() { return mobile; }
    public int getTemplateId() { return templateId; }
    public List<SmsParameter> getParameters() { return parameters; }
}