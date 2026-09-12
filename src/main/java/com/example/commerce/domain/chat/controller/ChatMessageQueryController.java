package com.example.commerce.domain.chat.controller;

import com.example.commerce.common.response.ApiResponse;
import com.example.commerce.domain.auth.entity.AuthUser;
import com.example.commerce.domain.chat.dto.response.ChatMessageSliceResponse;
import com.example.commerce.domain.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat-rooms/{chatRoomId}/messages")
@RequiredArgsConstructor
public class ChatMessageQueryController {

    private static final String DIRECTION_AFTER = "AFTER";
    private static final int DEFAULT_SIZE = 20;

    private final ChatMessageService chatMessageService;

    @GetMapping
    public ResponseEntity<ApiResponse<ChatMessageSliceResponse>> getMessages(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long chatRoomId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "BEFORE") String direction) {

        boolean isAfter = DIRECTION_AFTER.equalsIgnoreCase(direction);

        ChatMessageSliceResponse response = chatMessageService.getMessages(
                chatRoomId, authUser.getUserId(), cursor, size, isAfter);

        return ResponseEntity.ok(ApiResponse.ok("메시지 조회에 성공했습니다.", response));
    }
}