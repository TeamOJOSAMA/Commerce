package com.example.commerce.domain.refund.dto;

import com.example.commerce.domain.refund.entity.RefundType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record RefundRequest (
        @NotNull(message = "결제 ID는 필수입니다.")
        Long paymentId,

        @NotNull(message = "환불 유형은 필수입니다. (FULL / PARTIAL)")
        RefundType refundType,

        @NotEmpty(message = "환불 사유는 필수입니다.")
        String reason,

        @Valid
        List<Item> items        // PARTIAL 일 때만 사용, FULL 이면 무시
) {
    public record Item(
            @NotNull(message = "주문 항목 ID는 필수입니다.")
            Long orderItemId,

            @NotNull @Positive(message = "환불 수량은 1 이상이어야 합니다.")
            Integer quantity
    ) {}

    @AssertTrue(message = "부분 환불은 환불 항목이 하나 이상 필요합니다.")
    public boolean isItemPresentForPartial() {
        if (refundType != RefundType.PARTIAL) {
            return true;
        }
        return items != null && !items.isEmpty();
    }
}