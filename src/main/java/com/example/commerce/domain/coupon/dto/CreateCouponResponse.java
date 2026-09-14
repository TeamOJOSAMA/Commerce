package com.example.commerce.domain.coupon.dto;

import com.example.commerce.domain.coupon.entity.Coupon;
import com.example.commerce.domain.coupon.entity.CouponStatus;

import java.time.LocalDateTime;

public record CreateCouponResponse(
        Long couponId,
        String couponName,
        Integer discountRate,
        Integer minimumOrderAmount,
        Integer maximumDiscountAmount,
        Integer totalQuantity,
        Integer issuedQuantity,
        CouponStatus status,
        LocalDateTime issueStartsAt,
        LocalDateTime issueEndsAt
) {
    public static CreateCouponResponse from(Coupon coupon) {
        return new CreateCouponResponse(
                coupon.getId(),
                coupon.getName(),
                coupon.getDiscountRate(),
                coupon.getMinimumOrderAmount(),
                coupon.getMaximumDiscountAmount(),
                coupon.getTotalQuantity(),
                coupon.getIssuedQuantity(),
                coupon.getStatus(),
                coupon.getIssueStartsAt(),
                coupon.getIssueEndsAt()
        );
    }
}
