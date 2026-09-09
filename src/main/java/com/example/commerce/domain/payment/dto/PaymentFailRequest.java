package com.example.commerce.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentFailRequest (
        @NotBlank(message = "실패 사유는 필수입니다.")
        String failReason
){

}