package com.example.commerce.domain.chat.dto.response;

import com.example.commerce.domain.chat.entity.ChatMessage;
import com.example.commerce.domain.chat.entity.MessageType;

import java.time.LocalDateTime;

// 메시지 내역 조회와 STOMP 브로드캐스트에서 공통으로 사용
public record ChatMessageResponse(
        Long chatMessageId,
        Long chatRoomId,
        Long senderId,
        String senderName,
        MessageType messageType,
        String content,
        LocalDateTime createdAt
) {

    public static ChatMessageResponse from(ChatMessage chatMessage) {
        return new ChatMessageResponse(
                chatMessage.getId(),
                chatMessage.getChatRoom().getId(),
                chatMessage.getSender().getId(),
                chatMessage.getSender().getName(),
                chatMessage.getMessageType(),
                chatMessage.getContent(),
                chatMessage.getCreatedAt()
        );
    }
}