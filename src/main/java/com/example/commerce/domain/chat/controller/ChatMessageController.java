package com.example.commerce.domain.chat.controller;

import com.example.commerce.domain.auth.entity.AuthUser;
import com.example.commerce.domain.chat.dto.request.ChatMessageSendRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private static final String SUBSCRIBE_DESTINATION = "/sub/chat-rooms/";

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat-rooms/{chatRoomId}/messages")
    public void sendMessage(@DestinationVariable Long chatRoomId,
                            ChatMessageSendRequest request,
                            Principal principal) {
        AuthUser authUser = extractAuthUser(principal);

        log.info("메시지 수신: roomId={}, senderId={}, content={}",
                chatRoomId, authUser.getUserId(), request.content());

        // TODO: 채팅방 접근 권한 검증 및 메시지 영속화 추가
        messagingTemplate.convertAndSend(SUBSCRIBE_DESTINATION + chatRoomId, request);
    }

    private AuthUser extractAuthUser(Principal principal) {
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) principal;

        return (AuthUser) authentication.getPrincipal();
    }
}