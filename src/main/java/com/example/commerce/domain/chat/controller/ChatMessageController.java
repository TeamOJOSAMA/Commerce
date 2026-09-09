package com.example.commerce.domain.chat.controller;

import com.example.commerce.domain.chat.dto.request.ChatMessageSendRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private static final String SUBSCRIBE_DESTINATION = "/sub/chat-rooms/";

    private final SimpMessagingTemplate messagingTemplate;

    // 클라이언트는 /pub/chat-rooms/{chatRoomId}/messages 로 발행한다
    @MessageMapping("/chat-rooms/{chatRoomId}/messages")
    public void sendMessage(@DestinationVariable Long chatRoomId,
                            ChatMessageSendRequest request) {
        log.info("메시지 수신: roomId={}, content={}", chatRoomId, request.content());

        // TODO: 인증 적용 후 Principal 로 발신자 식별, 메시지 영속화 추가
        messagingTemplate.convertAndSend(SUBSCRIBE_DESTINATION + chatRoomId, request);
    }
}