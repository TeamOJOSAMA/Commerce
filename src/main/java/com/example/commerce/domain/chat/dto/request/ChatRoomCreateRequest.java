package com.example.commerce.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRoomCreateRequest(

        @NotBlank(message = "문의 제목은 필수입니다.")
        @Size(max = 30, message = "문의 제목은 30자를 초과할 수 없습니다.")
        String title
) {
}