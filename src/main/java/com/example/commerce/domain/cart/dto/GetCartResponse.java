package com.example.commerce.domain.cart.dto;

import com.example.commerce.domain.cart.entity.Cart;

import java.time.LocalDateTime;
import java.util.List;

public record GetCartResponse(
        Long cartId,
        List<Items> items,
        Long totalPrice,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static GetCartResponse from(Cart cart) {

        List<Items> items = cart.getCartItems().stream()
                .map(cartItem -> {
                    long subtotal = cartItem.getQuantity() * cartItem.getProduct().getPrice();

                    return new Items(
                            cartItem.getId(),
                            cartItem.getProduct().getId(),
                            cartItem.getProduct().getName(),
                            cartItem.getProduct().getPrice(),
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

    public record Items(
            Long cartItemId,
            Long productId,
            String productName,
            Long price,
            Integer quantity,
            Long subtotal
    ) {
    }
}
