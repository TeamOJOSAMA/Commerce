package com.example.commerce.domain.coupon.controller;

import com.example.commerce.common.response.ApiResponse;
import com.example.commerce.common.response.PageResponse;
import com.example.commerce.domain.coupon.dto.CreateCouponRequest;
import com.example.commerce.domain.coupon.dto.CouponResponse;
import com.example.commerce.domain.coupon.dto.SearchCouponRequest;
import com.example.commerce.domain.coupon.dto.UpdateCouponRequest;
import com.example.commerce.domain.coupon.dto.UpdateCouponResponse;
import com.example.commerce.domain.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/admin/coupons")
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(
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

    @GetMapping("/admin/coupons")
    public ResponseEntity<ApiResponse<PageResponse<CouponResponse>>> getAllCoupons(
            Pageable pageable,
            @Valid @ModelAttribute SearchCouponRequest request // @ModelAttribute = 바인딩 방식 명시
    ) {
        return ResponseEntity
                .ok(ApiResponse
                        .ok(
                                "모든 쿠폰을 열람했습니다.",
                                couponService.getAllCoupons(pageable, request)
                        ));
    }

    @PatchMapping("/admin/coupons/{couponId}")
    public ResponseEntity<ApiResponse<UpdateCouponResponse>> updateCoupon(
            @PathVariable("couponId") Long couponId,
            @Valid @RequestBody UpdateCouponRequest request
    ) {
        return ResponseEntity
                .ok(ApiResponse
                        .ok(
                                "쿠폰 정보를 수정했습니다.",
                                couponService.updateCoupon(couponId, request)
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
