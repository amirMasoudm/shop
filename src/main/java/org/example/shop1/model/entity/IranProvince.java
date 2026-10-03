package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "iran_provinces")
public class IranProvince {
    @Id
    private String id; // آیدی عددی پست به عنوان شناسه
    private String name;

    public IranProvince() {}
    public IranProvince(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}