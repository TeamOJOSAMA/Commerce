package com.example.commerce.domain.order.dto;

import com.example.commerce.domain.order.entity.Order;
import com.example.commerce.domain.order.entity.OrderStatus;

import java.time.LocalDateTime;

// 목록 화면에 필요한 식별자·상태·수량·결제금액만 반환하고 항목 상세와 결제 상태는 생략한다.
public record OrderSummaryResponse(
        Long orderId,
        String orderNumber,
        OrderStatus status,
        long totalQuantity,
        long paymentAmount,
        LocalDateTime createdAt
) {
    // 항목 수가 아니라 각 주문 항목 수량의 합계를 총수량으로 표시한다.
    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalQuantity(),
                order.getPaymentAmount(),
                order.getCreatedAt()
        );
    }
}
