package com.example.commerce.domain.payment.dto;

import com.example.commerce.domain.payment.entity.Payment;
import com.example.commerce.domain.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponse (
        Long id,
        Long orderId,
        Long amount,
        PaymentStatus status,
        String failReason,
        LocalDateTime paidAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getFailReason(),
                payment.getPaidAt()
        );
    }
}
