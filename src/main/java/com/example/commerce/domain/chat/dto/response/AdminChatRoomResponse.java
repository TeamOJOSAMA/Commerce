package com.example.commerce.domain.chat.dto.response;

import com.example.commerce.domain.chat.entity.ChatRoom;
import com.example.commerce.domain.chat.entity.InquiryStatus;

import java.time.LocalDateTime;

// 관리자가 전체 문의 목록을 조회할 때 사용
public record AdminChatRoomResponse(
        Long chatRoomId,
        String title,
        InquiryStatus inquiryStatus,
        Long customerId,
        String customerName,
        Long assigneeId,
        String assigneeName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AdminChatRoomResponse from(ChatRoom chatRoom) {
        boolean hasAssignee = chatRoom.getAssignee() != null;

        return new AdminChatRoomResponse(
                chatRoom.getId(),
                chatRoom.getTitle(),
                chatRoom.getInquiryStatus(),
                chatRoom.getCustomer().getId(),
                chatRoom.getCustomer().getName(),
                hasAssignee ? chatRoom.getAssignee().getId() : null,
                hasAssignee ? chatRoom.getAssignee().getName() : null,
                chatRoom.getCreatedAt(),
                chatRoom.getUpdatedAt()
        );
    }
}