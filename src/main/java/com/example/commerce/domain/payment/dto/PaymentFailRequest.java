package com.example.commerce.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentFailRequest (
        @NotBlank(message = "실패 사유는 필수입니다.")
        @Size(max = 50, message = "실패 사유는 50자 이하여야 합니다.")
        String failReason
){

}