package com.example.commerce.domain.coupon.repository;

import com.example.commerce.domain.coupon.entity.UserCoupon;
import com.example.commerce.domain.coupon.entity.UserCouponStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long>, UserCouponRepositoryCustom {

    /**
     * 사용기한이 지난 사용 가능한 쿠폰을 만료 상태로 일괄 변경한다.
     * 예약 또는 사용 완료된 쿠폰은 변경하지 않는다.
     *
     * @return 만료 처리된 사용자 쿠폰 수
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        update UserCoupon uc
           set uc.status = :expiredStatus,
               uc.updatedAt = :now
         where uc.status = :availableStatus
           and uc.expiresAt <= :now
        """)
    int expireAvailableCoupons(
            @Param("availableStatus") UserCouponStatus availableStatus,
            @Param("expiredStatus") UserCouponStatus expiredStatus,
            @Param("now") LocalDateTime now
    );

    // 조회용 할인 계산에서 원본 쿠폰의 할인 정책도 함께 가져온다.
    @EntityGraph(attributePaths = "coupon")
    Optional<UserCoupon> findByIdAndUserId(Long id, Long userId);

    // 예약·사용·해제 전에 발급 쿠폰을 잠근다. 상태 검사와 변경은 같은 트랜잭션에서 수행한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select userCoupon from UserCoupon userCoupon where userCoupon.id = :id and userCoupon.user.id = :userId")
    Optional<UserCoupon> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    // 쿠폰 삭제 전 발급 이력 존재 여부 확인
    boolean existsByCouponId(Long couponId);

    // 중복 발급 방지용
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);
}
