package com.example.commerce.domain.product.dto;

import com.example.commerce.domain.product.entity.ProductCategory;
import com.example.commerce.domain.product.entity.ProductStatus;

public record UpdateProductRequest(
        String name,
        String description,
        ProductCategory category,
        Long price,
        ProductStatus status,
        EventInfoRequest eventInfo
) {
}