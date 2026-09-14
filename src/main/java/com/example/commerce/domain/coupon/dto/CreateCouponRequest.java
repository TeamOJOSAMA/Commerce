package com.example.commerce.domain.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;

public record CreateCouponRequest(
        @NotBlank @Max(100) String name,
        @NotNull @Positive Integer discountRate,
        @NotNull @Positive Integer minimumOrderAmount,
        @NotNull @Positive Integer maximumDiscountAmount,
        @NotNull @Positive Integer totalQuantity
) {
}
