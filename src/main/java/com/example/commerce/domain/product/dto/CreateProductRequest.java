package com.example.commerce.domain.product.dto;

import com.example.commerce.domain.product.entity.ProductCategory;

public record CreateProductRequest(
        String name,
        String description,
        ProductCategory category,
        Long price,
        int stock,
        EventInfoRequest eventInfo
) {
}
