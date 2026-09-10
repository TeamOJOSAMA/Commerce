package com.example.commerce.domain.order.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

// 사용자·상품 가격·수량은 서버에서 읽고, 요청에는 선택한 장바구니 항목과 발급 쿠폰 ID만 받는다.
public record CreateOrderRequest(
        // null·빈 목록은 전체 장바구니를 뜻한다. 목록 자체는 허용하되 원소의 null은 거부한다.
        List<@NotNull Long> cartItemIds,
        // 원본 쿠폰이 아닌 사용자에게 발급된 쿠폰의 ID이며, null이면 쿠폰을 적용하지 않는다.
        Long userCouponId
) {
}
