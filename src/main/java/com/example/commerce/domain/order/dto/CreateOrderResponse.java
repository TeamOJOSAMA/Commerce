package com.example.commerce.domain.order.dto;

import com.example.commerce.domain.order.entity.Order;
import com.example.commerce.domain.payment.entity.Payment;
import com.example.commerce.domain.refund.entity.Refund;

/**
 * 주문 생성 직후의 주문 상태·금액과 후속 결제 승인에 필요한 정보를 제공한다.
 *
 * <p>멱등성 키로 기존 주문을 그대로 돌려줄 때도 같은 형태를 사용한다. 그 주문은 이미 취소되었거나
 * 결제가 실패한 상태일 수 있으므로 {@code displayStatus}를 반드시 함께 내려 클라이언트가 새로
 * 만들어진 주문과 구분할 수 있게 한다. 이 값이 없으면 화면은 취소된 주문에 결제창을 띄우고
 * 승인에서 INVALID_ORDER_STATUS를 받게 된다.</p>
 */
public record CreateOrderResponse(
        Long orderId,
        String orderNumber,
        // PAYMENT_PENDING이 아니면 결제를 진행할 수 없는 주문이다.
        OrderDisplayStatus displayStatus,
        // 결제창에 표시할 주문명이다. 항목이 여럿이면 "상품명 외 N건"이다.
        String orderName,
        long totalQuantity,
        // 쿠폰 할인을 적용하기 전의 주문 총액이다.
        long totalAmount,
        long couponDiscountAmount,
        // totalAmount에서 couponDiscountAmount를 뺀 실제 결제 대상 금액이다.
        long paymentAmount,
        // 승인·실패 API에 사용할 결제 ID다. 결제가 없는 주문이면 null이다.
        Long paymentId
) {
    // 새로 만든 주문의 환불은 항상 null이고, 멱등 재응답에서만 값이 있을 수 있다.
    public static CreateOrderResponse from(Order order, Payment payment, Refund refund) {
        return new CreateOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                OrderDisplayStatus.of(order.getStatus(), order.getCancelReason(),
                        refund == null ? null : refund.getStatus()),
                order.getOrderName(),
                order.getTotalQuantity(),
                order.getTotalAmount(),
                order.getCouponDiscountAmount(),
                order.getPaymentAmount(),
                payment == null ? null : payment.getId()
        );
    }
}
