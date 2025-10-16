package org.example.shop1.model.entity;



import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "categories")
public class Category {

    @Id
    private String id;
    private String name;
    private String parentId; // nullable
    private List<String> ancestors = new ArrayList<>();
    private Integer level = 0;
    private List<String> childrenIds = new ArrayList<>();

    // --- Constructors ---
    public Category() {}

    public Category(String name, String parentId, List<String> ancestors, Integer level) {
        this.name = name;
        this.parentId = parentId;
        this.ancestors = ancestors;
        this.level = level;
    }

    public Category(String id, String name, String parentId, List<String> ancestors, Integer level, List<String> childrenIds) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.ancestors = ancestors;
        this.level = level;
        this.childrenIds = childrenIds;
    }

    public List<String> getChildrenIds() {
        return childrenIds;
    }

    public void setChildrenIds(List<String> childrenIds) {
        this.childrenIds = childrenIds;
    }

    // --- Getters & Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public List<String> getAncestors() { return ancestors; }
    public void setAncestors(List<String> ancestors) { this.ancestors = ancestors; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
}
