package com.example.commerce.domain.product.dto;

import com.example.commerce.domain.product.entity.ProductCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateProductRequest(
        @NotBlank
        String name,
        @NotBlank
        String description,
        @NotBlank
        ProductCategory category,
        @PositiveOrZero
        Long price,
        @PositiveOrZero
        int stock,
        EventInfoRequest eventInfo
) {
}
