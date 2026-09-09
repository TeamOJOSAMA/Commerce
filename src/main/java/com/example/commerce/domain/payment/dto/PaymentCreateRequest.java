package com.example.commerce.domain.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentCreateRequest (
        @NotNull(message = "주문 ID는 필수입니다.")
        Long orderId,

        @NotNull(message = "결제 금액은 필수입니다.")
        @Positive(message = "결제 금액은 0보다 커야 합니다.") // @Positive 금액이 0이하인 요청 자체를 컨트롤러 진입 전에 차단
        Long amount
){

}