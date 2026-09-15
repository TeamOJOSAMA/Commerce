package com.example.commerce.domain.coupon.scheduler;

import com.example.commerce.domain.coupon.entity.CouponStatus;
import com.example.commerce.domain.coupon.entity.UserCouponStatus;
import com.example.commerce.domain.coupon.repository.CouponRepository;
import com.example.commerce.domain.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 쿠폰 정책과 사용자 쿠폰의 만료 상태를 주기적으로 갱신한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponExpirationScheduler {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    /**
     * 매분 만료 시각이 지난 쿠폰 정책과 사용자 쿠폰을 만료 처리한다.
     */
    @Transactional
    @Scheduled(
            cron = "${coupon.expiration.cron:0 * * * * *}",
            zone = "Asia/Seoul"
    )
    public void expireCoupons() {
        LocalDateTime now = LocalDateTime.now();

        // 발급 종료 시각이 지난 쿠폰 정책을 EXPIRED로 변경
        int expiredCouponCount = couponRepository.expireCouponPolicies(CouponStatus.EXPIRED, now);

        // 사용기한이 지난 AVAILABLE 사용자 쿠폰을 EXPIRED로 변경
        int expiredUserCouponCount = userCouponRepository.expireAvailableCoupons(
                UserCouponStatus.AVAILABLE,
                UserCouponStatus.EXPIRED,
                now
        );

        // 실제 변경된 쿠폰이 있을 때만 처리 결과 기록
        if (expiredCouponCount > 0 || expiredUserCouponCount > 0) {
            log.info(
                    "쿠폰 만료 처리 완료: 쿠폰 정책={}, 사용자 쿠폰={}",
                    expiredCouponCount,
                    expiredUserCouponCount
            );
        }
    }
}