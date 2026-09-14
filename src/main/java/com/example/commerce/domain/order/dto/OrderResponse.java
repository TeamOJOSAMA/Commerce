package com.example.commerce.domain.order.dto;

import com.example.commerce.domain.order.entity.Order;
import com.example.commerce.domain.order.entity.OrderItem;
import com.example.commerce.domain.payment.entity.Payment;
import com.example.commerce.domain.refund.entity.Refund;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 주문 스냅샷과 현재 진행 상태를 보여 주는 상세 응답이다.
 *
 * <p>결제·환불은 상태를 그대로 싣지 않고 {@link OrderDisplayStatus} 하나로 접는다. 화면이 조합 규칙을
 * 다시 구현하지 않게 하려는 것이며, 결제 자체의 내역(금액·승인 시각·실패 사유)은
 * {@code GET /api/payments/{paymentId}}가 답한다. 대신 다음 API를 호출할 진입 키인
 * {@code paymentId}·{@code refundId}는 파생할 수 없으므로 그대로 내려준다.</p>
 */
public record OrderResponse(
        Long orderId,
        String orderNumber,
        // 주문·취소 사유·환불을 합친 하나의 진행 상태다.
        OrderDisplayStatus displayStatus,
        // 목록과 결제창에 표시할 요약 주문명이다. 항목이 여럿이면 "상품명 외 N건"이다.
        String orderName,
        List<OrderItemResponse> items,
        long totalQuantity,
        long totalAmount,
        // 적용한 발급 쿠폰의 ID다. 쿠폰 이름·할인율은 쿠폰 조회 API가 제공한다.
        Long userCouponId,
        long couponDiscountAmount,
        long paymentAmount,
        // 결제 승인·실패 API와 환불 접수(RefundRequest.paymentId)의 진입 키다.
        // orderId로 결제를 찾는 API가 없어 이 값을 얻을 경로는 주문 응답뿐이다.
        Long paymentId,
        // 환불 완료 API의 진입 키다. 환불 이력이 없으면 null이다.
        Long refundId,
        LocalDateTime createdAt,
        LocalDateTime canceledAt
) {
    // 결제·환불이 아직 없는 주문도 조회할 수 있도록 둘 다 nullable로 받는다.
    public static OrderResponse from(Order order, Payment payment, Refund refund) {
        // 항목별 쿠폰 할인 배분은 주문이 계산한다. 화면과 부분 환불이 같은 기준을 쓰도록 응답에 함께 싣는다.
        Map<OrderItem, Long> couponDiscountShares = order.calculateCouponDiscountShares();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                OrderDisplayStatus.of(order.getStatus(), order.getCancelReason(),
                        refund == null ? null : refund.getStatus()),
                order.getOrderName(),
                order.getOrderItems().stream()
                        .map(orderItem -> OrderItemResponse.from(
                                orderItem, couponDiscountShares.getOrDefault(orderItem, 0L)))
                        .toList(),
                order.getTotalQuantity(),
                order.getTotalAmount(),
                order.getUserCouponId(),
                order.getCouponDiscountAmount(),
                order.getPaymentAmount(),
                payment == null ? null : payment.getId(),
                refund == null ? null : refund.getId(),
                order.getCreatedAt(),
                order.getCanceledAt()
        );
    }
}
