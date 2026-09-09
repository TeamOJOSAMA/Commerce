package com.example.commerce.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// STOMP 로 발행되는 메시지 페이로드
// senderId 는 클라이언트가 보내지 않고 Principal 에서 식별한다
public record ChatMessageSendRequest(

        @NotBlank(message = "메시지 내용은 필수입니다.")
        @Size(max = 1000, message = "메시지는 1000자를 초과할 수 없습니다.")
        String content
) {
}