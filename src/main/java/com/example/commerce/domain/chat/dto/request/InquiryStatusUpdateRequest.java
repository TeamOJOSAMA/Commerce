package com.example.commerce.domain.chat.dto.request;

import com.example.commerce.domain.chat.entity.InquiryStatus;
import jakarta.validation.constraints.NotNull;

public record InquiryStatusUpdateRequest(

        @NotNull(message = "변경할 문의 상태는 필수입니다.")
        InquiryStatus inquiryStatus
) {
}