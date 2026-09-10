package com.example.commerce.domain.product.dto;

import com.example.commerce.domain.product.entity.ProductCategory;
import com.example.commerce.domain.product.entity.ProductStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateProductRequest(
        @NotBlank
        String name,
        @NotBlank
        String description,
        @NotBlank
        ProductCategory category,
        @PositiveOrZero
        Long price,
        @NotBlank
        ProductStatus status ,
        EventInfoRequest eventInfo
) {
}