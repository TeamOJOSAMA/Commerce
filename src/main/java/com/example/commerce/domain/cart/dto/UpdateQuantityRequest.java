package com.example.commerce.domain.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateQuantityRequest(@NotNull @PositiveOrZero Integer quantity) {
}
