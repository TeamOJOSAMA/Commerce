package com.example.commerce.domain.chat.dto.response;

import com.example.commerce.domain.chat.entity.ChatRoom;
import com.example.commerce.domain.chat.entity.InquiryStatus;

import java.time.LocalDateTime;

// 고객이 본인 문의 목록을 조회할 때 사용
public record CustomerChatRoomResponse(
        Long chatRoomId,
        String title,
        InquiryStatus inquiryStatus,
        String assigneeName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static CustomerChatRoomResponse from(ChatRoom chatRoom) {
        return new CustomerChatRoomResponse(
                chatRoom.getId(),
                chatRoom.getTitle(),
                chatRoom.getInquiryStatus(),
                // 상담원 배정 전에는 null
                chatRoom.getAssignee() == null ? null : chatRoom.getAssignee().getName(),
                chatRoom.getCreatedAt(),
                chatRoom.getUpdatedAt()
        );
    }
}