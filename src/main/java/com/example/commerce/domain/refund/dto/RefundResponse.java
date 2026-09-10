package com.example.commerce.domain.refund.dto;

import com.example.commerce.domain.refund.entity.Refund;
import com.example.commerce.domain.refund.entity.RefundStatus;

import java.time.LocalDateTime;
import java.util.List;

public record RefundResponse (
        Long id,
        Long paymentId,
        String reason,
        RefundStatus status,
        Long totalRefundAmount,
        List<Item> items,
        LocalDateTime refundAt
) {
    public record Item(Long orderItemId, Integer quantity, Long refundAmount) {}

    public static RefundResponse from(Refund refund) {
        List<Item> items = refund.getRefundItems().stream()
                .map(ri -> new Item(ri.getOrderItem().getId(),ri.getQuantity(),ri.getRefundAmount())).toList();

        return new RefundResponse(
                refund.getId(),
                refund.getPayment().getId(),
                refund.getReason(),
                refund.getStatus(),
                refund.getTotalRefundAmount(),
                items,
                refund.getRefundedAt()
        );
    }
}