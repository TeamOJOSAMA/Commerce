package com.example.commerce.domain.product.dto;

import com.example.commerce.domain.product.entity.ProductCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateProductRequest(
        @NotBlank
        String name,
        @NotBlank
        String description,
        @NotNull
        ProductCategory category,
        @PositiveOrZero
        Long price,
        @PositiveOrZero
        int stock,
        EventInfoRequest eventInfo
) {
}
