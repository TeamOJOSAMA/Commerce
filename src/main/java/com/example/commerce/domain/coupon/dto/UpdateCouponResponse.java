package com.example.commerce.domain.coupon.dto;

import com.example.commerce.domain.coupon.entity.Coupon;
import com.example.commerce.domain.coupon.entity.CouponStatus;

import java.time.LocalDateTime;

public record UpdateCouponResponse(
        Long couponId,
        String name,
        Integer discountRate,
        Integer minimumOrderAmount,
        Integer maximumDiscountAmount,
        Integer totalQuantity,
        CouponStatus status,
        LocalDateTime updatedAt
) {
    public static UpdateCouponResponse from(Coupon coupon) {
        return new UpdateCouponResponse(
                coupon.getId(),
                coupon.getName(),
                coupon.getDiscountRate(),
                coupon.getMinimumOrderAmount(),
                coupon.getMaximumDiscountAmount(),
                coupon.getTotalQuantity(),
                coupon.getStatus(),
                coupon.getUpdatedAt()
        );
    }
}
