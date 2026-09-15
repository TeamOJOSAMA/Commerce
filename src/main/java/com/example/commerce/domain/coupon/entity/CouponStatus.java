package com.example.commerce.domain.coupon.entity;

public enum CouponStatus {

    // 쿠폰 정책 활성 상태
    ACTIVE {
        @Override
        public boolean canTransitTo(CouponStatus target) {
            return target == INACTIVE;
        }
    },

    // 운영상 비활성 상태
    INACTIVE{
        @Override
        public boolean canTransitTo(CouponStatus target) {
            return target == ACTIVE;
        }
    },

    // 쿠폰 정책 자체의 발급/유효 기간 종료
    EXPIRED{
        @Override
        public boolean canTransitTo(CouponStatus target) {
            return target == ACTIVE || target == INACTIVE;
        }
    };

    public abstract boolean canTransitTo(CouponStatus target);
}
