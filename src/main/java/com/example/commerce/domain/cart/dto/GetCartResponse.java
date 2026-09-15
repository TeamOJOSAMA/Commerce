package com.example.commerce.domain.cart.dto;

import com.example.commerce.domain.cart.entity.Cart;
import com.example.commerce.domain.event.entity.Event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record GetCartResponse(
        Long cartId,
        List<Items> items,
        Long totalPrice,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    // 장바구니 내용물 조회. 상품이 진행 중 이벤트에 걸려 있으면 상세/목록과 같은 할인가를 보여준다.
    public static GetCartResponse from(Cart cart, Map<Long, Event> eventsByProductId) {

        List<Items> items = cart.getCartItems().stream()
                .map(cartItem -> {
                    Long productId = cartItem.getProduct().getId();
                    Event event = eventsByProductId.get(productId);
                    Long price = cartItem.getProduct().getPrice();
                    Long eventPrice = event == null ? null : event.getEventPrice();
                    Integer discountRate = event == null ? null : event.getDiscountRate();
                    long unitPrice = eventPrice == null ? price : eventPrice;
                    long subtotal = cartItem.getQuantity() * unitPrice;

                    return new Items(
                            cartItem.getId(),
                            productId,
                            cartItem.getProduct().getName(),
                            price,
                            eventPrice,
                            discountRate,
                            cartItem.getQuantity(),
                            subtotal
                    );
                })
                .toList();

        long totalPrice = items.stream()
                .mapToLong(Items::subtotal)
                .sum();

        return new GetCartResponse(cart.getId(), items, totalPrice, cart.getCreatedAt(), cart.getUpdatedAt());
    }

    // 장바구니 자체가 없는 경우
    public static GetCartResponse empty() {
        return new GetCartResponse(null, List.of(), 0L, null, null);
    }

    public record Items(
            Long cartItemId,
            Long productId,
            String productName,
            // 이벤트 여부와 무관한 상품 원가다. 이벤트 상품은 할인 전 금액(취소선 표시용)으로 쓴다.
            Long price,
            // 진행 중 이벤트가 있을 때만 값이 있다. 없으면 일반 상품이다.
            Long eventPrice,
            Integer discountRate,
            Integer quantity,
            // eventPrice가 있으면 eventPrice, 없으면 price 기준의 수량 곱이다.
            Long subtotal
    ) {
    }
}
