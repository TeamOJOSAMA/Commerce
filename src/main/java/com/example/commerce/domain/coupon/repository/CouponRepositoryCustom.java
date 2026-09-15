package com.example.commerce.domain.coupon.repository;

import com.example.commerce.domain.coupon.dto.SearchCouponRequest;
import com.example.commerce.domain.coupon.dto.CouponResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CouponRepositoryCustom {

    /**
     * 쿠폰을 검색 조건에 따라 페이징 조회한다.
     * 각 검색 조건이 null이면 해당 조건은 조회에서 제외한다.
     */
    Page<CouponResponse> searchCouponByConditionPage(Pageable pageable, SearchCouponRequest request);
}
