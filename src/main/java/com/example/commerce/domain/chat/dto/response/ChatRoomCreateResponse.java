package com.example.commerce.domain.chat.dto.response;

import com.example.commerce.domain.chat.entity.ChatRoom;
import com.example.commerce.domain.chat.entity.InquiryStatus;

import java.time.LocalDateTime;

public record ChatRoomCreateResponse(
        Long chatRoomId,
        String title,
        InquiryStatus inquiryStatus,
        Long customerId,
        String customerName,
        LocalDateTime createdAt
) {

    public static ChatRoomCreateResponse from(ChatRoom chatRoom) {
        return new ChatRoomCreateResponse(
                chatRoom.getId(),
                chatRoom.getTitle(),
                chatRoom.getInquiryStatus(),
                chatRoom.getCustomer().getId(),
                chatRoom.getCustomer().getName(),
                chatRoom.getCreatedAt()
        );
    }
}