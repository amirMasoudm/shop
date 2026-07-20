package org.example.shop1.model.dto;

import java.util.ArrayList;
import java.util.List;

public class CategoryResponseDto {
    private String id;
    private String name;
    private String parentId;
    private Integer level;
    private Integer position;
    // *** این فیلد را اضافه کنید ***
    private List<String> filterKeys = new ArrayList<>();

    private List<CategoryResponseDto> children = new ArrayList<>();

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    // *** Getter & Setter جدید ***
    public List<String> getFilterKeys() { return filterKeys; }
    public void setFilterKeys(List<String> filterKeys) { this.filterKeys = filterKeys; }

    public List<CategoryResponseDto> getChildren() { return children; }
    public void setChildren(List<CategoryResponseDto> children) { this.children = children; }
}