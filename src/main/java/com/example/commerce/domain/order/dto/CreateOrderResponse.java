package com.example.commerce.domain.order.dto;

import com.example.commerce.domain.order.entity.Order;
import com.example.commerce.domain.order.entity.OrderStatus;
import com.example.commerce.domain.payment.entity.Payment;

// 주문 생성 직후의 주문 상태·금액과 함께 후속 결제 승인에 필요한 식별자를 제공한다.
public record CreateOrderResponse(
        Long orderId,
        // POST /api/payments/{paymentId}/approve 또는 /fail에서 사용하는 결제 ID다.
        Long paymentId,
        String orderNumber,
        OrderStatus status,
        // 상품 단가에 이벤트 가격은 반영되어 있지만 쿠폰 할인은 적용하기 전의 합계다.
        long totalAmount,
        long couponDiscountAmount,
        // totalAmount에서 couponDiscountAmount를 뺀 실제 결제 대상 금액이다.
        long paymentAmount
) {
    // 같은 트랜잭션에서 생성한 주문과 READY 결제를 조합한다.
    public static CreateOrderResponse from(Order order, Payment payment) {
        return new CreateOrderResponse(
                order.getId(),
                payment.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCouponDiscountAmount(),
                order.getPaymentAmount()
        );
    }
}
