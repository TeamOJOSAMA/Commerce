package com.example.commerce.domain.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateCouponRequest(
        @NotBlank String name,
        @NotNull @Positive Integer discountRate,
        @NotNull @Positive Integer minimumOrderAmount,
        @NotNull @Positive Integer maximumDiscountAmount,
        @NotNull @Positive Integer totalQuantity
) {
}
