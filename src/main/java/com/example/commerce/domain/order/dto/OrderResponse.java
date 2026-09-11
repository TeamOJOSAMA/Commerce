package com.example.commerce.domain.order.dto;

import com.example.commerce.domain.order.entity.Order;
import com.example.commerce.domain.order.entity.OrderStatus;
import com.example.commerce.domain.payment.entity.Payment;
import com.example.commerce.domain.payment.entity.PaymentStatus;

import java.time.LocalDateTime;
import java.util.List;

// 주문 스냅샷과 현재 결제 진행 상태를 함께 보여 주는 상세 응답이다.
public record OrderResponse(
        Long orderId,
        String orderNumber,
        OrderStatus status,
        List<OrderItemResponse> items,
        long totalQuantity,
        long totalAmount,
        Long userCouponId,
        long couponDiscountAmount,
        long paymentAmount,
        // 결제 내역이 없으면 null이다. 주문 상태와 별개로 결제 상태를 표시한다.
        PaymentStatus paymentStatus,
        // 미승인 결제이거나 결제 내역이 없으면 null이다.
        LocalDateTime paidAt,
        LocalDateTime createdAt,
        LocalDateTime canceledAt
) {
    // 결제 없는 기존 주문도 조회할 수 있도록 nullable 결제를 처리한다.
    public static OrderResponse from(Order order, Payment payment) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getOrderItems().stream()
                        .map(OrderItemResponse::from)
                        .toList(),
                order.getTotalQuantity(),
                order.getTotalAmount(),
                order.getUserCouponId(),
                order.getCouponDiscountAmount(),
                order.getPaymentAmount(),
                payment == null ? null : payment.getStatus(),
                payment == null ? null : payment.getPaidAt(),
                order.getCreatedAt(),
                order.getCanceledAt()
        );
    }
}
