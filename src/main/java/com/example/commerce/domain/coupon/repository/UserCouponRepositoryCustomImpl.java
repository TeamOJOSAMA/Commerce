package com.example.commerce.domain.coupon.repository;

import com.example.commerce.domain.coupon.dto.SearchUserCouponRequest;
import com.example.commerce.domain.coupon.dto.UserCouponResponse;
import com.example.commerce.domain.coupon.entity.UserCouponStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.example.commerce.domain.coupon.entity.QCoupon.coupon;
import static com.example.commerce.domain.coupon.entity.QUserCoupon.userCoupon;

@RequiredArgsConstructor
public class UserCouponRepositoryCustomImpl implements UserCouponRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<UserCouponResponse> searchUserCoupons(
            Long userId,
            Pageable pageable,
            SearchUserCouponRequest request
    ) {
        BooleanExpression[] conditions = {userCoupon.user.id.eq(userId), userCouponStatusEq(request.status())};

        List<UserCouponResponse> responses = queryFactory
                .select(Projections.constructor(
                        UserCouponResponse.class,
                        userCoupon.id,
                        coupon.id,
                        coupon.name,
                        coupon.discountRate,
                        coupon.minimumOrderAmount,
                        coupon.maximumDiscountAmount,
                        userCoupon.status,
                        userCoupon.issuedAt,
                        userCoupon.expiresAt,
                        userCoupon.usedAt
                ))
                .from(userCoupon)
                .join(userCoupon.coupon, coupon)
                .where(conditions)
                .orderBy(userCoupon.issuedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(userCoupon.count())
                .from(userCoupon)
                .where(conditions)
                .fetchOne();

        return new PageImpl<>(responses, pageable, total != null ? total : 0L);
    }

    private BooleanExpression userCouponStatusEq(UserCouponStatus status) {
        return status != null ? userCoupon.status.eq(status) : null;
    }
}
