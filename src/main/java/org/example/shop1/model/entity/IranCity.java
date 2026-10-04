package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "iran_cities")
public class IranCity {
    @Id
    private String id; // آیدی عددی پست به عنوان شناسه
    private String name;
    private String provinceId; // آیدی والد (استان)

    public IranCity() {}
    public IranCity(String id, String name, String provinceId) {
        this.id = id;
        this.name = name;
        this.provinceId = provinceId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProvinceId() { return provinceId; }
    public void setProvinceId(String provinceId) { this.provinceId = provinceId; }
}