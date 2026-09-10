package com.example.commerce.domain.coupon.repository;

import com.example.commerce.domain.coupon.entity.UserCoupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    // 조회용 할인 계산에서 원본 쿠폰의 할인 정책도 함께 가져온다.
    @EntityGraph(attributePaths = "coupon")
    Optional<UserCoupon> findByIdAndUserId(Long id, Long userId);

    // 예약·사용·해제 전에 발급 쿠폰을 잠근다. 상태 검사와 변경은 같은 트랜잭션에서 수행한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select userCoupon from UserCoupon userCoupon where userCoupon.id = :id and userCoupon.user.id = :userId")
    Optional<UserCoupon> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);
}
