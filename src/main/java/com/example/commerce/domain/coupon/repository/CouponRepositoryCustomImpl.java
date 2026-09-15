package com.example.commerce.domain.coupon.repository;

import com.example.commerce.domain.coupon.dto.CouponResponse;
import com.example.commerce.domain.coupon.dto.SearchCouponRequest;
import com.example.commerce.domain.coupon.entity.CouponStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.example.commerce.domain.coupon.entity.QCoupon.coupon;

@RequiredArgsConstructor
public class CouponRepositoryCustomImpl implements CouponRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<CouponResponse> searchCouponByConditionPage(Pageable pageable, SearchCouponRequest request) {

        List<CouponResponse> responses = queryFactory
                .select(Projections.constructor(
                        CouponResponse.class,
                        coupon.id,
                        coupon.name,
                        coupon.discountRate,
                        coupon.minimumOrderAmount,
                        coupon.maximumDiscountAmount,
                        coupon.totalQuantity,
                        coupon.issuedQuantity,
                        coupon.status,
                        coupon.issueStartsAt,
                        coupon.issueEndsAt
                        ))
                .from(coupon)
                .where(
                        couponStatusEq(request.couponStatus()),
                        discountRateEq(request.discountRate()),
                        minimumOrderAmountEq(request.minimumOrderAmount()),
                        maximumDiscountAmountEq(request.maximumDiscountAmount())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(coupon.count())
                .from(coupon)
                .where(
                        couponStatusEq(request.couponStatus()),
                        discountRateEq(request.discountRate()),
                        minimumOrderAmountEq(request.minimumOrderAmount()),
                        maximumDiscountAmountEq(request.maximumDiscountAmount())
                )
                .fetchOne();

        return new PageImpl<>(responses, pageable, total != null ? total : 0L);
    }

    private BooleanExpression couponStatusEq(CouponStatus status) {
        return status != null ? coupon.status.eq(status) : null;
    }

    private BooleanExpression discountRateEq(Integer discountRate) {
        return discountRate != null ? coupon.discountRate.eq(discountRate) : null;
    }

    private BooleanExpression minimumOrderAmountEq(Integer minimumOrderAmount) {
        return minimumOrderAmount != null ? coupon.minimumOrderAmount.eq(minimumOrderAmount) : null;
    }

    private BooleanExpression maximumDiscountAmountEq(Integer maximumDiscountAmount) {
        return maximumDiscountAmount != null ? coupon.maximumDiscountAmount.eq(maximumDiscountAmount) : null;
    }
}
