package com.example.commerce.domain.coupon.controller;

import com.example.commerce.common.response.ApiResponse;
import com.example.commerce.common.response.PageResponse;
import com.example.commerce.domain.auth.entity.AuthUser;
import com.example.commerce.domain.coupon.dto.CreateCouponRequest;
import com.example.commerce.domain.coupon.dto.CouponResponse;
import com.example.commerce.domain.coupon.dto.SearchUserCouponRequest;
import com.example.commerce.domain.coupon.dto.UserCouponResponse;
import com.example.commerce.domain.coupon.dto.SearchCouponRequest;
import com.example.commerce.domain.coupon.dto.UpdateCouponRequest;
import com.example.commerce.domain.coupon.dto.UpdateCouponResponse;
import com.example.commerce.domain.coupon.facade.CouponFacade;
import com.example.commerce.domain.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final CouponFacade couponFacade;

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
                                "모든 쿠폰을 조회했습니다.",
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

    @DeleteMapping("/admin/coupons/{couponId}")
    public ResponseEntity<ApiResponse<CouponResponse>> deleteCoupon(@PathVariable("couponId") Long couponId) {

        couponService.deleteCoupon(couponId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/coupons")
    public ResponseEntity<ApiResponse<PageResponse<CouponResponse>>> getActiveCoupons(
            Pageable pageable,
            @Valid @ModelAttribute SearchCouponRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        "활성 쿠폰을 조회했습니다.",
                        couponService.getActiveCoupons(pageable, request)
                )
        );
    }

    @PostMapping("/coupons/{couponId}/issue")
    public ResponseEntity<ApiResponse<UserCouponResponse>> createUserCoupon(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long couponId
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse
                        .ok(
                                "쿠폰을 발급했습니다.",
                                couponFacade.createUserCoupon(authUser.getUserId(), couponId)
                        ));
    }

    @GetMapping("/users/me/coupons")
    public ResponseEntity<ApiResponse<PageResponse<UserCouponResponse>>> getUserCoupons(
            @AuthenticationPrincipal AuthUser authuser,
            Pageable pageable,
            @ModelAttribute SearchUserCouponRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        "내 쿠폰을 조회했습니다.",
                        couponService.getUserCoupons(authuser.getUserId(), pageable, request)
                )
        );
    }
}
