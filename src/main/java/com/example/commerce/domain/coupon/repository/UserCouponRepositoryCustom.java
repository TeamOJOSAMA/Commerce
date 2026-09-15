package com.example.commerce.domain.coupon.repository;

import com.example.commerce.domain.coupon.dto.SearchUserCouponRequest;
import com.example.commerce.domain.coupon.dto.UserCouponResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserCouponRepositoryCustom {

    /**
     * 사용자의 발급 쿠폰을 상태 조건에 따라 페이징 조회한다.
     * 상태가 null이면 해당 사용자의 모든 쿠폰을 조회한다.
     */
    Page<UserCouponResponse> searchUserCoupons(Long userId, Pageable pageable, SearchUserCouponRequest request);
}
