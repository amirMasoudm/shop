package org.example.shop1.model.dto;

import org.example.shop1.model.entity.Product;
import org.springframework.data.domain.Page;
import java.util.List;

/**
 * خروجیِ سرچِ عمومی. محصولات به‌صورتِ {@link PublicProductDto} برمی‌گردند، نه انتیتیِ خام —
 * این اندپوینت permitAll است و قبلاً کلِ فیلدهایِ داخلی را بیرون می‌داد.
 */
public class ProductSearchResponseDto {
    private Page<PublicProductDto> products;
    private List<FilterResponseDto> availableFilters; // لیست فیلترهای سایدبار

    public ProductSearchResponseDto(Page<Product> products, List<FilterResponseDto> availableFilters) {
        // ساختارِ Page (content/last/totalElements/…) دست‌نخورده می‌ماند؛ فقط آیتم‌ها نگاشت می‌شوند
        this.products = products.map(PublicProductDto::of);
        this.availableFilters = availableFilters;
    }

    public Page<PublicProductDto> getProducts() { return products; }
    public List<FilterResponseDto> getAvailableFilters() { return availableFilters; }
}