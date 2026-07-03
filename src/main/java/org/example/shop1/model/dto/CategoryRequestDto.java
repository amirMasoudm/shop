package org.example.shop1.model.dto;

import java.util.List;

public class CategoryRequestDto {

    private String name;
    private String parentId;
    private String newParentId;
    // *** این فیلد حیاتی است ***
    private String type;

    // --- Getters & Setters ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getNewParentId() { return newParentId; }
    public void setNewParentId(String newParentId) { this.newParentId = newParentId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    // اضافه کردن به DTO ورودی
    private List<String> filterKeys;

    public List<String> getFilterKeys() { return filterKeys; }
    public void setFilterKeys(List<String> filterKeys) { this.filterKeys = filterKeys; }
}