package com.example.commerce.domain.coupon.entity;

import com.example.commerce.common.entity.BaseEntity;
import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseEntity {

    private static final int COUPON_ISSUE_PERIOD_MONTHS = 6; // 6개월 유효

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name; // 쿠폰 이름

    @Column(name = "discount_rate", nullable = false)
    private Integer discountRate; // 할인율

    @Column(name = "minimum_order_amount")
    private Integer minimumOrderAmount; // 할인 적용 가능한 최소 금액

    @Column(name = "maximum_discount_amount")
    private Integer maximumDiscountAmount; // 할인 금액 상한선

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity; // 쿠폰 재고

    @Column(name = "issued_quantity", nullable = false)
    private Integer issuedQuantity; // 발급된 쿠폰 수

    @Column(name = "issue_starts_at", nullable = false)
    private LocalDateTime issueStartsAt; // 쿠폰 발급 시작 시각

    @Column(name = "issue_ends_at", nullable = false)
    private LocalDateTime issueEndsAt; // 쿠폰 발급 종료 시각

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponStatus status;

    public Coupon(String name, Integer discountRate, Integer minimumOrderAmount,
                  Integer maximumDiscountAmount, Integer totalQuantity) {
        this.name = name;
        this.discountRate = discountRate;
        this.minimumOrderAmount = minimumOrderAmount;
        this.maximumDiscountAmount = maximumDiscountAmount;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0; // default:0 스키마 반영
        this.status = CouponStatus.ACTIVE; // 쿠폰 생성 시 활성화 상태로 초기화
        this.issueStartsAt = LocalDateTime.now();
        this.issueEndsAt = issueStartsAt.plusMonths(COUPON_ISSUE_PERIOD_MONTHS);
    }

    public void updateName(String newName) {
        this.name = newName;
    }

    public void updateDiscountRate(Integer newDiscountRate) {
        this.discountRate = newDiscountRate;
    }

    public void updateMinimumOrderAmount(Integer newMinimumOrderAmount) {
        this.minimumOrderAmount = newMinimumOrderAmount;
    }

    public void updateMaximumDiscountAmount(Integer newMaximumDiscountAmount) {
        this.maximumDiscountAmount = newMaximumDiscountAmount;
    }

    public void updateTotalQuantity(Integer newTotalQuantity) {

        // 쿠폰 총 재고가 발급된 쿠폰 수 이상이어야함
        if (newTotalQuantity < issuedQuantity)
            throw new BusinessException(ErrorCode.INVALID_COUPON_QUANTITY);

        this.totalQuantity = newTotalQuantity;
    }

    public void activeCoupon() {

        CouponStatus tempStatus = this.status; // updateStatus 순간 DB가 변경되므로 기존 상태 임시저장

        updateStatus(CouponStatus.ACTIVE);

        // 만료일 경우 재활성시 기한도 갱신
        if (tempStatus == CouponStatus.EXPIRED) {
            this.issueStartsAt = LocalDateTime.now();
            this.issueEndsAt = issueStartsAt.plusMonths(COUPON_ISSUE_PERIOD_MONTHS);
        }
    }

    public void inactiveCoupon() {
        updateStatus(CouponStatus.INACTIVE);
    }

    private void updateStatus(CouponStatus newStatus) {
        if (newStatus == null)
            throw new BusinessException(ErrorCode.INVALID_COUPON_STATUS);

        if (status != newStatus && !status.canTransitTo(newStatus))
            throw new BusinessException(
                    ErrorCode.INVALID_COUPON_STATUS,
                    "쿠폰 상태를 " + status + "에서 " + newStatus + "로 변경할 수 없습니다."
            );

        this.status = newStatus;
    }

    /**
     * 사용자에게 쿠폰을 발급한다.
     * 상태와 남은 수량을 확인한 뒤 발급 수량을 증가시킨다.
     */
    public void issue() {

        // 활성 상태의 쿠폰만 발급 가능
        if (status != CouponStatus.ACTIVE)
            throw new BusinessException(ErrorCode.COUPON_NOT_ACTIVE);

        // 발급 수량이 전체 수량에 도달했다면 추가 발급 불가
        if (issuedQuantity >= totalQuantity)
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);

        // 스케줄러 실행 전이라도 발급 종료 시각이 지났다면 발급을 거부
        if (!LocalDateTime.now().isBefore(issueEndsAt))
            throw new BusinessException(ErrorCode.COUPON_NOT_ACTIVE);

        // 발급 수량 증가
        issuedQuantity++;
    }
}
