package com.example.commerce.domain.refund.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record RefundRequest (
        @NotNull(message = "결제 ID는 필수입니다.")
        Long paymentId,

        @NotBlank(message = "환불 사유는 필수입니다.")
        String reason,

        @NotEmpty(message = "환불 항목은 하나 이상 필요합니다.")
        @Valid
        List<Item> items
) {
    public record Item(
            @NotNull(message = "주문 항목 ID는 필수입니다.")
            Long orderItemId,

            @NotNull @Positive(message = "환불 수량은 1 이상이어야 합니다.")
            Integer quantity
    ) {}
}