package com.example.commerce.domain.cart.dto;

import com.example.commerce.domain.cart.entity.CartItem;

import java.time.LocalDateTime;

public record UpdateQuantityResponse(Long cartItemId, Long productId, Integer quantity, LocalDateTime updatedAt) {

    public static UpdateQuantityResponse from(CartItem cartItem) {
        return new UpdateQuantityResponse(
                cartItem.getId(),
                cartItem.getProduct().getId(),
                cartItem.getQuantity(),
                cartItem.getUpdatedAt()
        );
    }
}
