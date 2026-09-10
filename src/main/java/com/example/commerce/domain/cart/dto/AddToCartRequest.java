package com.example.commerce.domain.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AddToCartRequest(@NotNull @PositiveOrZero Integer quantity) {
}
