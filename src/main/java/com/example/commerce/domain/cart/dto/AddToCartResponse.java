package com.example.commerce.domain.cart.dto;

import com.example.commerce.domain.cart.entity.CartItem;

import java.time.LocalDateTime;

public record AddToCartResponse(Long cartItemId, Long productId, Integer quantity, LocalDateTime createdAt) {

    public static AddToCartResponse from(CartItem cartItem) {
        return new AddToCartResponse(
                cartItem.getId(),
                cartItem.getProductId(),
                cartItem.getQuantity(),
                cartItem.getCart().getCreatedAt()
        );
    }
}
