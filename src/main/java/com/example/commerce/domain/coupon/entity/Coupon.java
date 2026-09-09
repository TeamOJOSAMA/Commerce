package com.example.commerce.domain.coupon.entity;

import com.example.commerce.common.entity.BaseEntity;
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
        this.status = CouponStatus.ACTIVE; // 쿠폰 발급 오픈
        this.issueStartsAt = LocalDateTime.now();
        this.issueEndsAt = issueStartsAt.plusMonths(6); // 6개월 유효
    }

    public void expiredCoupon() {
        this.status = CouponStatus.EXPIRED;
    }

    public void activeCoupon() {
        this.status = CouponStatus.ACTIVE;
    }

    public void inactiveCoupon() {
        this.status = CouponStatus.INACTIVE;
    }
}
