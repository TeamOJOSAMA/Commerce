package com.example.commerce.domain.coupon.dto;

import com.example.commerce.domain.coupon.entity.UserCoupon;
import com.example.commerce.domain.coupon.entity.UserCouponStatus;

import java.time.LocalDateTime;

public record CreateUserCouponResponse(
        Long userCouponId,
        Long couponId,
        String couponName,
        Integer discountRate,
        Integer minimumOrderAmount,
        Integer maximumDiscountAmount,
        UserCouponStatus status,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt,
        LocalDateTime usedAt
) {
    // 쿠폰을 받는 주체가 회원이므로 UserCoupon을 인수로 받음
    public static CreateUserCouponResponse from(UserCoupon userCoupon) {
        return new CreateUserCouponResponse(
                userCoupon.getId(),
                userCoupon.getCoupon().getId(),
                userCoupon.getCoupon().getName(),
                userCoupon.getCoupon().getDiscountRate(),
                userCoupon.getCoupon().getMinimumOrderAmount(),
                userCoupon.getCoupon().getMaximumDiscountAmount(),
                userCoupon.getStatus(),
                userCoupon.getIssuedAt(),
                userCoupon.getExpiresAt(),
                userCoupon.getUsedAt()
        );
    }
}
