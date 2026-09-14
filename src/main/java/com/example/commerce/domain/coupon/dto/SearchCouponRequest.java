package com.example.commerce.domain.coupon.dto;

import com.example.commerce.domain.coupon.entity.CouponStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

public record SearchCouponRequest(
        CouponStatus couponStatus,
        @Min(1) @Max(100) Integer discountRate,
        @PositiveOrZero Integer minimumOrderAmount,
        @PositiveOrZero Integer maximumDiscountAmount
) {
}
