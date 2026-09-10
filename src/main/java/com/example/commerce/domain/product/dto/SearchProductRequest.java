package com.example.commerce.domain.product.dto;

import com.example.commerce.domain.product.entity.ProductCategory;
import com.example.commerce.domain.product.entity.ProductStatus;

public record SearchProductRequest(
         String name,
         ProductCategory category,
         Long minPrice,
         Long maxPrice,
         ProductStatus status

) {
}
