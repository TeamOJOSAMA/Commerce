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
        // 이벤트 적용 전 상품 원가다. 상세/장바구니와 같은 취소선 표시를 위해 이벤트 항목일 때만 값이 있다.
        // 이벤트 항목은 쿠폰 할인 대상이 아니므로(couponDiscountShare가 항상 0) 두 할인 표시가 겹치지 않는다.
        Long originalPrice,
        Integer discountRate,
        int quantity,
        // 주문 당시 단가 × 수량이며 쿠폰 할인 배분액은 빼지 않은 금액이다.
        long subTotal,
        // 주문 전체 쿠폰 할인액 중 이 항목에 배분된 몫이다. 주문 시점에 저장된 값이며 이벤트 항목은 0이다.
        long couponDiscountShare,
        // subTotal에서 couponDiscountShare를 뺀 이 항목의 실결제액이다. 부분 환불 금액 계산의 기준이다.
        long paidAmount
) {
    public static OrderItemResponse from(OrderItem orderItem, Long originalPrice, Integer discountRate) {
        return new OrderItemResponse(
                orderItem.getId(),
                orderItem.getProductId(),
                orderItem.getEventId(),
                orderItem.getProductName(),
                orderItem.getUnitPrice(),
                originalPrice,
                discountRate,
                orderItem.getQuantity(),
                orderItem.getSubTotal(),
                orderItem.getCouponDiscountShare(),
                orderItem.getPaidAmount()
        );
    }
}
