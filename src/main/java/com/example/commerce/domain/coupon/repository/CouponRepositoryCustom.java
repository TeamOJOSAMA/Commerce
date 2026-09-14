package com.example.commerce.domain.coupon.repository;

import com.example.commerce.domain.coupon.dto.SearchCouponRequest;
import com.example.commerce.domain.coupon.dto.CouponResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CouponRepositoryCustom {

    Page<CouponResponse> searchCouponByConditionPage(Pageable pageable, SearchCouponRequest request);
}
