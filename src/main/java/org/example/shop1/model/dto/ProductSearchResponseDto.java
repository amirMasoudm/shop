package org.example.shop1.model.dto;

import org.example.shop1.model.entity.Product;
import org.springframework.data.domain.Page;
import java.util.List;

public class ProductSearchResponseDto {
    private Page<Product> products;
    private List<FilterResponseDto> availableFilters; // لیست فیلترهای سایدبار

    public ProductSearchResponseDto(Page<Product> products, List<FilterResponseDto> availableFilters) {
        this.products = products;
        this.availableFilters = availableFilters;
    }

    public Page<Product> getProducts() { return products; }
    public List<FilterResponseDto> getAvailableFilters() { return availableFilters; }
}