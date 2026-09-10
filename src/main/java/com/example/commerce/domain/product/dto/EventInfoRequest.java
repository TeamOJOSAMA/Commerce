package com.example.commerce.domain.product.dto;

import java.time.LocalDateTime;

public record EventInfoRequest(
        int discountRate,
        Long eventPrice,
        LocalDateTime startAt,
        int durationHours
) {
}