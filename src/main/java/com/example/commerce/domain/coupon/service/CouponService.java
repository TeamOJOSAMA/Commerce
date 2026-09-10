package com.example.commerce.domain.coupon.service;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.coupon.entity.Coupon;
import com.example.commerce.domain.coupon.entity.CouponStatus;
import com.example.commerce.domain.coupon.entity.UserCoupon;
import com.example.commerce.domain.coupon.entity.UserCouponStatus;
import com.example.commerce.domain.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 할인 계산과 사용자 쿠폰의 AVAILABLE → RESERVED → USED 전이를 연결한다.
 * 주문과의 연결은 Order.userCouponId가 담당하며 승인·취소 호출자는 주문을 먼저 잠가야 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private static final int MAX_DISCOUNT_RATE = 100;

    private final UserCouponRepository userCouponRepository;

    // 조회용 계산이며 쿠폰 상태를 변경하지 않는다.
    public long calculateDiscount(Long userId, Long userCouponId, long couponEligibleAmount) {
        validateInput(userId, userCouponId, couponEligibleAmount);

        UserCoupon userCoupon = userCouponRepository.findByIdAndUserId(userCouponId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_COUPON_NOT_FOUND));
        return calculateDiscount(userCoupon, couponEligibleAmount);
    }

    /** 주문 생성 트랜잭션 안에서 할인 계산과 예약을 함께 수행해 중복 예약을 막는다. */
    @Transactional
    public long reserveCoupon(Long userId, Long userCouponId, long couponEligibleAmount) {
        validateInput(userId, userCouponId, couponEligibleAmount);
        // 상태를 검사하기 전에 잠가 여러 주문이 같은 AVAILABLE 상태를 함께 사용하지 못하게 한다.
        UserCoupon userCoupon = getOwnedCouponForUpdate(userId, userCouponId);
        long discountAmount = calculateDiscount(userCoupon, couponEligibleAmount);
        userCoupon.reserve();
        return discountAmount;
    }

    /** 결제 승인 트랜잭션에서 예약된 쿠폰을 사용 확정한다. 결제 실패 처리에서는 호출하지 않는다. */
    @Transactional
    public void useCoupon(Long userId, Long userCouponId) {
        UserCoupon userCoupon = getOwnedCouponForUpdate(userId, userCouponId);
        // 예약 시 확보한 할인 조건을 유지하고 결제 승인 시 사용을 확정한다.
        userCoupon.use();
    }

    /** 대기 주문 취소 시 예약을 해제한다. 재취소 여부는 주문 잠금 아래에서 호출자가 검사한다. */
    @Transactional
    public void releaseCoupon(Long userId, Long userCouponId) {
        UserCoupon userCoupon = getOwnedCouponForUpdate(userId, userCouponId);
        // 해제 시점에 이미 만료되었다면 다시 AVAILABLE이 되지 않도록 현재 시각을 전달한다.
        userCoupon.release(LocalDateTime.now());
    }

    // 소유자 조건을 포함한 잠금 조회로 다른 사용자의 쿠폰 상태 변경을 막는다.
    private UserCoupon getOwnedCouponForUpdate(Long userId, Long userCouponId) {
        return userCouponRepository.findByIdAndUserIdForUpdate(userCouponId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_COUPON_NOT_FOUND));
    }

    // HTTP 밖의 내부 호출에서도 잘못된 ID와 음수 할인 대상 금액을 거부한다.
    private void validateInput(Long userId, Long userCouponId, long couponEligibleAmount) {
        if (userId == null || userId <= 0 || userCouponId == null || userCouponId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (couponEligibleAmount < 0) {
            throw new BusinessException(ErrorCode.INVALID_COUPON_ELIGIBLE_AMOUNT);
        }
    }

    /** 조회와 예약이 공유하는 할인 검증·계산이다. 이벤트 상품 금액은 호출자가 제외한다. */
    private long calculateDiscount(UserCoupon userCoupon, long couponEligibleAmount) {
        LocalDateTime now = LocalDateTime.now();
        // 발급 기간(issueStartsAt/issueEndsAt)과 발급받은 쿠폰의 사용 기간은 구분한다.
        if (userCoupon.getStatus() == UserCouponStatus.EXPIRED || !now.isBefore(userCoupon.getExpiresAt())) {
            throw new BusinessException(ErrorCode.USER_COUPON_EXPIRED);
        }
        // 다른 주문이 예약했거나 이미 사용한 쿠폰, 아직 발급 시각이 되지 않은 쿠폰은 적용하지 않는다.
        if (userCoupon.getStatus() != UserCouponStatus.AVAILABLE || now.isBefore(userCoupon.getIssuedAt())) {
            throw new BusinessException(ErrorCode.USER_COUPON_UNAVAILABLE);
        }

        // 발급 쿠폰이 유효해도 원본 쿠폰 정책이 비활성화되었다면 새 예약을 허용하지 않는다.
        Coupon coupon = userCoupon.getCoupon();
        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.COUPON_INACTIVE);
        }
        validateDiscountPolicy(coupon);

        // 최소 주문 금액은 전체 주문액이 아니라 쿠폰 적용 대상인 일반 상품 합계에 적용한다.
        Integer minimumOrderAmount = coupon.getMinimumOrderAmount();
        if (minimumOrderAmount != null && couponEligibleAmount < minimumOrderAmount) {
            throw new BusinessException(ErrorCode.COUPON_MINIMUM_AMOUNT_NOT_MET);
        }

        // 정수 곱셈의 오버플로를 피하고 원 미만은 버린다. 계산 결과가 0원인 경우도 허용한다.
        long discountAmount = BigDecimal.valueOf(couponEligibleAmount)
                .multiply(BigDecimal.valueOf(coupon.getDiscountRate()))
                .divide(BigDecimal.valueOf(MAX_DISCOUNT_RATE), 0, RoundingMode.DOWN)
                .longValueExact();

        // 최대 할인액이 설정된 경우에만 계산된 할인액의 상한으로 사용한다.
        Integer maximumDiscountAmount = coupon.getMaximumDiscountAmount();
        return maximumDiscountAmount == null
                ? discountAmount
                : Math.min(discountAmount, maximumDiscountAmount.longValue());
    }

    // 잘못 저장된 할인율이나 한도가 실제 주문 금액 계산에 들어가지 않도록 방어한다.
    private void validateDiscountPolicy(Coupon coupon) {
        Integer discountRate = coupon.getDiscountRate();
        if (discountRate == null || discountRate <= 0 || discountRate > MAX_DISCOUNT_RATE) {
            throw new BusinessException(ErrorCode.INVALID_COUPON_POLICY);
        }
        // null은 제한 없음이며, 설정된 최소 금액과 최대 할인액은 0 이상이어야 한다.
        if (coupon.getMinimumOrderAmount() != null && coupon.getMinimumOrderAmount() < 0) {
            throw new BusinessException(ErrorCode.INVALID_COUPON_POLICY);
        }
        if (coupon.getMaximumDiscountAmount() != null && coupon.getMaximumDiscountAmount() < 0) {
            throw new BusinessException(ErrorCode.INVALID_COUPON_POLICY);
        }
    }
}
