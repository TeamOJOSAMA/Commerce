package com.example.commerce.domain.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateCouponRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Positive Integer discountRate,
        @NotNull @Positive Integer minimumOrderAmount,
        @NotNull @Positive Integer maximumDiscountAmount,
        @NotNull @Positive Integer totalQuantity
) {
}
