package com.example.commerce.domain.cart.dto;

import com.example.commerce.domain.cart.entity.Cart;

import java.time.LocalDateTime;
import java.util.List;

public record GetCartResponse(
        Long cartId,
        List<Items> items,
        Integer totalPrice,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static GetCartResponse from(Cart cart) {

        List<Items> items = cart.getCartItems().stream()
                .map(cartItem -> {
                    int subtotal = cartItem.getQuantity(); // TODO: 가격 연동되면 여기에 가격 곱할 예정

                    return new Items(
                            cartItem.getId(),
                            cartItem.getProductId(),
                            null, // TODO: Product 엔티티와 연동되면 getProductName()으로 변경 예정
                            null, // TODO: Product 엔티티와 연동되면 getPrice()로 변경 예정
                            cartItem.getQuantity(),
                            subtotal
                    );
                })
                .toList();

        int totalPrice = items.stream()
                .mapToInt(Items::subtotal)
                .sum();

        return new GetCartResponse(cart.getId(), items, totalPrice, cart.getCreatedAt(), cart.getUpdatedAt());
    }

    public record Items(
            Long cartItemId,
            Long productId,
            String productName,
            Integer price,
            Integer quantity,
            Integer subtotal
    ) {
    }
}
