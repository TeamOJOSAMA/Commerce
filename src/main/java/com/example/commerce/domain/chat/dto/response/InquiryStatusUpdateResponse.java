package com.example.commerce.domain.chat.dto.response;

import com.example.commerce.domain.chat.entity.ChatRoom;
import com.example.commerce.domain.chat.entity.InquiryStatus;

import java.time.LocalDateTime;

public record InquiryStatusUpdateResponse(
        Long chatRoomId,
        InquiryStatus inquiryStatus,
        Long assigneeId,
        String assigneeName,
        LocalDateTime updatedAt
) {

    public static InquiryStatusUpdateResponse from(ChatRoom chatRoom) {
        boolean hasAssignee = chatRoom.getAssignee() != null;

        return new InquiryStatusUpdateResponse(
                chatRoom.getId(),
                chatRoom.getInquiryStatus(),
                hasAssignee ? chatRoom.getAssignee().getId() : null,
                hasAssignee ? chatRoom.getAssignee().getName() : null,
                chatRoom.getUpdatedAt()
        );
    }
}