package com.example.commerce.domain.coupon.controller;

import com.example.commerce.common.response.ApiResponse;
import com.example.commerce.domain.coupon.dto.CreateCouponRequest;
import com.example.commerce.domain.coupon.dto.CreateCouponResponse;
import com.example.commerce.domain.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/admin/coupons")
    public ResponseEntity<ApiResponse<CreateCouponResponse>> createCoupon(
            @Valid @RequestBody CreateCouponRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse
                        .ok(
                                request.name() + " 쿠폰을 생성했습니다.",
                                couponService.createCoupon(request)
                        ));
    }

    // TODO: 쿠폰 발급 API 개발중
//    @PostMapping("/coupons/{couponId}/issue")
//    public ResponseEntity<ApiResponse<CreateUserCouponResponse>> createUserCoupon(
//            @PathVariable Long couponId
//    ) {
//
//    }
}
