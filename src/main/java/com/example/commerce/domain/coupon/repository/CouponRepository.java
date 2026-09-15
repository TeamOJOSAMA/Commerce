package com.example.commerce.domain.coupon.repository;

import com.example.commerce.domain.coupon.entity.Coupon;
import com.example.commerce.domain.coupon.entity.CouponStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long>, CouponRepositoryCustom {

    /**
     * 발급 종료 시각이 지난 쿠폰 정책을 만료 상태로 일괄 변경한다.
     *
     * @return 만료 처리된 쿠폰 정책 수
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        update Coupon c
           set c.status = :expiredStatus,
               c.updatedAt = :now
         where c.status <> :expiredStatus
           and c.issueEndsAt <= :now
        """)
    int expireCouponPolicies(@Param("expiredStatus") CouponStatus expiredStatus, @Param("now") LocalDateTime now);

    // 쿠폰 발급 시 같은 쿠폰의 재고가 동시에 차감되지 않도록 행 잠금 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c where c.id = :couponId")
    Optional<Coupon> findByIdForUpdate(@Param("couponId") Long couponId);
}