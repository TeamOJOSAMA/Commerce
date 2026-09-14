package com.example.commerce.domain.product.dto;

import com.example.commerce.domain.event.entity.Event;
import com.example.commerce.domain.product.entity.Product;
import com.example.commerce.domain.product.entity.ProductCategory;
import com.example.commerce.domain.product.entity.ProductStatus;
import java.io.Serializable;

public record ProductResponse(
        Long id,
        String name,
        String description,
        ProductCategory category,
        Long price,
        int stock,
        ProductStatus status,
        Long eventPrice,
        Integer discountRate

) implements Serializable {
    public static ProductResponse of(Product product, Event activeEvent) {
        Long eventPrice = (activeEvent != null) ? (long) activeEvent.getEventPrice() : null;
        Integer discountRate = (activeEvent != null) ? activeEvent.getDiscountRate() : null;

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                eventPrice,
                discountRate
        );
    }
}
