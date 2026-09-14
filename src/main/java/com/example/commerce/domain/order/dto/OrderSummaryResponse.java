package com.example.commerce.domain.order.dto;

import com.example.commerce.domain.order.entity.Order;
import com.example.commerce.domain.payment.entity.Payment;
import com.example.commerce.domain.refund.entity.Refund;

import java.time.LocalDateTime;

/**
 * 목록 화면에 필요한 식별·금액·진행 상태만 반환하고 항목 상세는 상세 조회에 맡긴다.
 * 진행 상태와 진입 키의 의미는 {@link OrderResponse}와 같다.
 */
public record OrderSummaryResponse(
        Long orderId,
        String orderNumber,
        // 주문·취소 사유·환불을 합친 하나의 진행 상태다.
        OrderDisplayStatus displayStatus,
        // 항목이 여럿이면 "상품명 외 N건"으로 요약한 주문명이다.
        String orderName,
        long totalQuantity,
        // 쿠폰 할인 전 총액이다. 목록에서 할인 표시가 필요해 결제 금액과 함께 반환한다.
        long totalAmount,
        long couponDiscountAmount,
        long paymentAmount,
        // 결제 승인·실패와 환불 접수의 진입 키다.
        Long paymentId,
        // 환불 완료 API의 진입 키다. 환불 이력이 없으면 null이다.
        Long refundId,
        LocalDateTime createdAt,
        LocalDateTime canceledAt
) {
    // 항목 수가 아니라 각 주문 항목 수량의 합계를 총수량으로 표시한다.
    public static OrderSummaryResponse from(Order order, Payment payment, Refund refund) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                OrderDisplayStatus.of(order.getStatus(), order.getCancelReason(),
                        refund == null ? null : refund.getStatus()),
                order.getOrderName(),
                order.getTotalQuantity(),
                order.getTotalAmount(),
                order.getCouponDiscountAmount(),
                order.getPaymentAmount(),
                payment == null ? null : payment.getId(),
                refund == null ? null : refund.getId(),
                order.getCreatedAt(),
                order.getCanceledAt()
        );
    }
}
