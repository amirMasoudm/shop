package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "categories")
public class Category {
    public Category() {}

    @Id
    private String id;
    private String name;
    private String parentId; // nullable
    private String type; // ONLINE یا WAREHOUSE (فیلد جدید)

    private List<String> ancestors = new ArrayList<>();
    private Integer level = 0;
    private List<String> childrenIds = new ArrayList<>();

    // --- Constructors ---
    // اضافه کردن فیلد جدید
    private List<String> filterKeys = new ArrayList<>(); // مثلا: ["سایز", "رنگ", "ولتاژ"]

    // Getter & Setter
    public List<String> getFilterKeys() { return filterKeys; }
    public void setFilterKeys(List<String> filterKeys) { this.filterKeys = filterKeys; }

    // --- Getters & Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<String> getAncestors() { return ancestors; }
    public void setAncestors(List<String> ancestors) { this.ancestors = ancestors; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public List<String> getChildrenIds() { return childrenIds; }
    public void setChildrenIds(List<String> childrenIds) { this.childrenIds = childrenIds; }
}