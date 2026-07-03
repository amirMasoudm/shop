package org.example.shop1.model.dto;

public class SmsParameter {
    private String name;
    private String value;

    public SmsParameter(String name, String value) {
        this.name = name;
        this.value = value;
    }
    // Getters...
    public String getName() { return name; }
    public String getValue() { return value; }
}