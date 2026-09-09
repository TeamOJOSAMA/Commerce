package com.example.commerce.domain.chat.controller;

import com.example.commerce.domain.auth.entity.AuthUser;
import com.example.commerce.domain.chat.dto.request.ChatMessageSendRequest;
import com.example.commerce.domain.chat.dto.response.ChatMessageResponse;
import com.example.commerce.domain.chat.service.ChatMessageService;
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

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat-rooms/{chatRoomId}/messages")
    public void sendMessage(@DestinationVariable Long chatRoomId,
                            ChatMessageSendRequest request,
                            Principal principal) {
        AuthUser authUser = extractAuthUser(principal);

        ChatMessageResponse response = chatMessageService.sendMessage(
                chatRoomId, authUser.getUserId(), request.content());

        messagingTemplate.convertAndSend(SUBSCRIBE_DESTINATION + chatRoomId, response);
    }

    private AuthUser extractAuthUser(Principal principal) {
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) principal;

        return (AuthUser) authentication.getPrincipal();
    }
}