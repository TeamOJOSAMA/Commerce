package com.example.commerce.domain.coupon.dto;

import com.example.commerce.domain.coupon.entity.CouponStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateCouponRequest(
        @Size(max = 100) String name,
        @Min(1) @Max(100) Integer discountRate, // CouponService가 0을 거부하므로 최솟값을 1로 설정
        @PositiveOrZero Integer minimumOrderAmount,
        @Positive Integer maximumDiscountAmount,
        @Positive Integer totalQuantity,
        CouponStatus status
) {
}
