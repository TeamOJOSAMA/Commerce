package com.example.commerce.domain.coupon.entity;

public enum UserCouponStatus {
    AVAILABLE,  // 발급되어 현재 사용 가능
    RESERVED,   // 주문/결제 대기 중 임시 점유
    USED,       // 결제 성공 후 최종 사용 완료
    EXPIRED     // 해당 사용자 쿠폰의 개별 만료일 경과
}
