package com.example.commerce.domain.coupon.dto;

import com.example.commerce.domain.coupon.entity.UserCouponStatus;

public record SearchUserCouponRequest(UserCouponStatus status) {
}

// 참고로 enum에 없는 값은 QueryDSL까지 내려가지 않고 Spring 바인딩 단계에서 차단됨