package com.example.commerce.domain.product.dto;

public record EventInfoRequest(
        int discountRate,
        Long eventPrice,
        int durationHours
) {
}
