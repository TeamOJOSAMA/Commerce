package com.example.commerce.domain.order.dto;

import com.example.commerce.domain.order.entity.OrderItem;

import java.util.List;

// 아직 저장하지 않은 주문의 예상 금액이다. 실제 주문 생성 시 가격과 이벤트를 다시 확인한다.
public record OrderPreviewResponse(
        List<OrderPreviewItemResponse> items,
        long totalQuantity,
        long totalAmount,
        // 이벤트가 적용되지 않은 일반 상품 금액만 합산한 쿠폰 할인 계산 기준이다.
        long couponEligibleAmount
) {
    // 호출자가 원래 목록을 수정해도 이미 만든 응답의 항목 구성이 달라지지 않도록 복사한다.
    public static OrderPreviewResponse from(
            List<OrderPreviewItemResponse> items,
            long totalQuantity,
            long totalAmount,
            long couponEligibleAmount
    ) {
        return new OrderPreviewResponse(
                List.copyOf(items), totalQuantity, totalAmount, couponEligibleAmount
        );
    }

    // 저장 전 항목이므로 orderItemId 대신 상품·이벤트와 예상 단가·수량을 제공한다.
    public record OrderPreviewItemResponse(
            Long productId,
            Long eventId,
            String productName,
            long unitPrice,
            int quantity,
            long subTotal
    ) {
        // Facade가 금액 범위를 검사하며 계산한 소계를 그대로 사용한다.
        public static OrderPreviewItemResponse from(OrderItem orderItem, long subTotal) {
            return new OrderPreviewItemResponse(
                    orderItem.getProductId(),
                    orderItem.getEventId(),
                    orderItem.getProductName(),
                    orderItem.getUnitPrice(),
                    orderItem.getQuantity(),
                    subTotal
            );
        }
    }
}
