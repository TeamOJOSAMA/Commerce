package com.example.commerce.domain.coupon.entity;

import com.example.commerce.common.entity.BaseEntity;
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

    public void reservedUserCoupon() {
        this.status = UserCouponStatus.RESERVED;
    }

    public void usedUserCoupon() {
        this.status = UserCouponStatus.USED;
        this.usedAt = LocalDateTime.now();
    }

    public void expiredUserCoupon() {
        this.status = UserCouponStatus.EXPIRED;
    }

    public void availableUserCoupon() {
        this.status = UserCouponStatus.AVAILABLE;
    }
}
