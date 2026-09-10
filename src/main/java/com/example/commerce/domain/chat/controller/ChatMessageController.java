package com.example.commerce.domain.chat.controller;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.response.ApiErrorResponse;
import com.example.commerce.domain.auth.entity.AuthUser;
import com.example.commerce.domain.chat.dto.request.ChatMessageSendRequest;
import com.example.commerce.domain.chat.dto.response.ChatMessageResponse;
import com.example.commerce.domain.chat.service.ChatBotService;
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
    private static final String STATUS_DESTINATION_SUFFIX = "/status";
    private static final String ERROR_DESTINATION_SUFFIX = "/errors";

    private final ChatMessageService chatMessageService;
    private final ChatBotService chatBotService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat-rooms/{chatRoomId}/messages")
    public void sendMessage(@DestinationVariable Long chatRoomId,
                            ChatMessageSendRequest request,
                            Principal principal) {
        AuthUser authUser = extractAuthUser(principal);

        try {
            // 고객 메시지를 먼저 저장하고 브로드캐스트한다
            ChatMessageResponse customerMessage = chatMessageService.sendMessage(
                    chatRoomId, authUser.getUserId(), request.content());
            broadcast(chatRoomId, customerMessage);

            replyIfBotTurn(chatRoomId, request.content());
        } catch (BusinessException e) {
            sendError(chatRoomId, e);
        }
    }

    // 봇 응대 중일 때만 자동응답한다
    private void replyIfBotTurn(Long chatRoomId, String content) {
        if (!chatBotService.isBotTurn(chatMessageService.getChatRoom(chatRoomId))) {
            return;
        }

        if (chatBotService.isEscalateRequest(content)) {
            broadcast(chatRoomId, chatBotService.escalate(chatRoomId));
            notifyStatusChanged(chatRoomId, "WAITING");

            return;
        }

        if (chatBotService.isCloseRequest(content)) {
            broadcast(chatRoomId, chatBotService.close(chatRoomId));
            notifyStatusChanged(chatRoomId, "COMPLETED");

            return;
        }

        if (chatBotService.isContinueRequest(content)) {
            broadcast(chatRoomId, chatBotService.resume(chatRoomId));

            return;
        }

        broadcast(chatRoomId, chatBotService.reply(chatRoomId, content));
    }

    private void broadcast(Long chatRoomId, ChatMessageResponse message) {
        messagingTemplate.convertAndSend(SUBSCRIBE_DESTINATION + chatRoomId, message);
    }

    private void notifyStatusChanged(Long chatRoomId, String status) {
        messagingTemplate.convertAndSend(
                SUBSCRIBE_DESTINATION + chatRoomId + STATUS_DESTINATION_SUFFIX, status);
    }

    private void sendError(Long chatRoomId, BusinessException e) {
        log.warn("STOMP 메시지 처리 실패: {}", e.getMessage());

        messagingTemplate.convertAndSend(
                SUBSCRIBE_DESTINATION + chatRoomId + ERROR_DESTINATION_SUFFIX,
                ApiErrorResponse.of(e.getErrorCode()));
    }

    private AuthUser extractAuthUser(Principal principal) {
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) principal;

        return (AuthUser) authentication.getPrincipal();
    }
}