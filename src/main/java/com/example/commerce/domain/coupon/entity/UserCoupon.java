package com.example.commerce.domain.coupon.entity;

import com.example.commerce.common.entity.BaseEntity;
import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon extends BaseEntity {

    private static final int USER_COUPON_VALIDITY_DAYS = 14; // 14일 유효

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserCouponStatus status;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt; // 쿠폰 발급일

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt; // 쿠폰 만료일

    @Column(name = "used_at")
    private LocalDateTime usedAt; // 쿠폰 사용일

    public UserCoupon(User user, Coupon coupon) {
        this.user = user;
        this.coupon = coupon;
        this.status = UserCouponStatus.AVAILABLE; // 쿠폰 발급시
        this.issuedAt = LocalDateTime.now();
        this.expiresAt = issuedAt.plusDays(USER_COUPON_VALIDITY_DAYS);
    }

    // 주문 대기 중 점유만 표시한다. 실제 사용 시각은 결제가 승인될 때 기록한다.
    public void reserve() {
        if (status != UserCouponStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.USER_COUPON_UNAVAILABLE);
        }
        this.status = UserCouponStatus.RESERVED;
    }

    // 예약된 쿠폰만 결제 승인으로 사용 확정할 수 있다.
    public void use() {
        if (status != UserCouponStatus.RESERVED) {
            throw new BusinessException(ErrorCode.USER_COUPON_UNAVAILABLE);
        }
        this.status = UserCouponStatus.USED;
        this.usedAt = LocalDateTime.now();
    }

    public void expiredUserCoupon() {
        this.status = UserCouponStatus.EXPIRED;
    }

    // 대기 주문 취소에 사용하는 예약 해제이며, USED 쿠폰의 환불 복구와는 구분한다.
    public void release(LocalDateTime now) {
        // 주문의 상태 검사와 재취소 방지는 호출 서비스가 담당한다.
        if (status != UserCouponStatus.RESERVED) {
            return;
        }
        // 예약 중 만료 시각이 지났으면 해제하더라도 다시 사용할 수 없게 한다.
        this.status = now.isBefore(expiresAt) ? UserCouponStatus.AVAILABLE : UserCouponStatus.EXPIRED;
    }
}
