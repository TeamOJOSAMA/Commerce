package com.example.commerce.domain.coupon.facade;

import com.example.commerce.domain.coupon.dto.CreateUserCouponResponse;
import com.example.commerce.domain.coupon.service.CouponService;
import com.example.commerce.domain.user.entity.User;
import com.example.commerce.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Facade가 Service를 둘러싸고 있다고 이해하면된다. Facade를 거치지 않는 트랜잭션이 있을 수 있다. */
@Component
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션으로 설정
@RequiredArgsConstructor
public class CouponFacade {

    private final UserService userService;
    private final CouponService couponService;

    @Transactional
    public CreateUserCouponResponse createUserCoupon(Long userId, Long couponId) {

        // 요청 userId에 해당하는 회원이 UserRepository에 있는가
        User user = userService.findUser(userId); // 없다면 404

        // 비즈니스 로직 진행
        return couponService.createUserCoupon(user, couponId);
    }
}
