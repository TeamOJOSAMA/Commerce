package com.example.commerce.domain.product.dto;

import com.example.commerce.domain.product.entity.Product;
import com.example.commerce.domain.product.entity.ProductCategory;
import com.example.commerce.domain.product.entity.ProductStatus;

public record CreateProductResponse(
        Long id,
        String name,
        String description,
        ProductCategory category,
        Long price,
        int stock,
        ProductStatus status,
        Long eventPrice,
        Integer discountRate
) {
    public static CreateProductResponse from(Product product) {
        return new CreateProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.getEventPrice(),
                product.getDiscountRate()
        );
    }
}