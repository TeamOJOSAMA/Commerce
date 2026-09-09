package com.example.commerce.domain.coupon.entity;

public enum CouponStatus {
    ACTIVE,     // 쿠폰 정책 활성 상태
    INACTIVE,   // 운영상 비활성 상태
    EXPIRED     // 쿠폰 정책 자체의 발급/유효 기간 종료
}
