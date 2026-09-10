package com.example.commerce.domain.product.dto;

import com.example.commerce.domain.product.entity.Product;
import com.example.commerce.domain.product.entity.ProductCategory;
import com.example.commerce.domain.product.entity.ProductStatus;

public record ProductResponse(
        Long id,
        String name,
        String description,
        ProductCategory category,
        Long price,
        int stock,
        ProductStatus status

) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getPrice(),
                product.getStock(),
                product.getStatus()
        );
    }
}
