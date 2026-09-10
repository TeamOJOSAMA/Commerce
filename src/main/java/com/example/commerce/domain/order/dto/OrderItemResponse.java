package com.example.commerce.domain.order.dto;

import com.example.commerce.domain.order.entity.OrderItem;

// 상품의 현재 정보가 아니라 주문 항목에 저장된 주문 당시 정보를 반환한다.
public record OrderItemResponse(
        Long orderItemId,
        Long productId,
        // 일반 상품은 null이며, 이벤트 상품은 주문에 적용한 이벤트의 ID다.
        Long eventId,
        String productName,
        long unitPrice,
        int quantity,
        long subTotal
) {
    // 소계는 주문 당시 단가 × 수량이며 주문 전체의 쿠폰 할인 배분액은 포함하지 않는다.
    public static OrderItemResponse from(OrderItem orderItem) {
        return new OrderItemResponse(
                orderItem.getId(),
                orderItem.getProductId(),
                orderItem.getEventId(),
                orderItem.getProductName(),
                orderItem.getUnitPrice(),
                orderItem.getQuantity(),
                orderItem.getSubTotal()
        );
    }
}
