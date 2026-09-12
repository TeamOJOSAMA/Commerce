package com.example.commerce.domain.chat.config;

import com.example.commerce.domain.auth.entity.AuthUser;
import com.example.commerce.domain.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 입장/퇴장은 별도 API 없이 STOMP 이벤트로 처리한다.
// SessionDisconnectEvent 는 브라우저 종료 같은 비정상 종료에서도 발생한다
@Slf4j
@Component
@RequiredArgsConstructor
public class StompEventListener {

    private static final String ROOM_DESTINATION_PREFIX = "/sub/chat-rooms/";

    private final ChatMessageService chatMessageService;

    // 연결이 끊겼을 때 어느 방에 있었는지 알아야 퇴장 메시지를 보낼 수 있다
    private final Map<String, Long> sessionRooms = new ConcurrentHashMap<>();

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        Long chatRoomId = extractChatRoomId(accessor.getDestination());
        if (chatRoomId == null) {
            return;
        }

        Long userId = extractUserId(accessor.getUser());
        if (userId == null) {
            return;
        }

        // 같은 세션이 이미 입장했다면 중복 발송하지 않는다
        if (sessionRooms.putIfAbsent(accessor.getSessionId(), chatRoomId) != null) {
            return;
        }

        chatMessageService.sendEnterMessage(chatRoomId, userId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        Long chatRoomId = sessionRooms.remove(accessor.getSessionId());
        if (chatRoomId == null) {
            return;
        }

        Long userId = extractUserId(accessor.getUser());
        if (userId == null) {
            return;
        }

        chatMessageService.sendLeaveMessage(chatRoomId, userId);
    }

    // 메시지 경로(/sub/chat-rooms/1)에서만 방 번호를 추출
    // 에러나 상태 경로(/sub/chat-rooms/1/errors)는 입장으로 보지 안ㅎ음
    private Long extractChatRoomId(String destination) {
        if (destination == null || !destination.startsWith(ROOM_DESTINATION_PREFIX)) {
            return null;
        }

        String suffix = destination.substring(ROOM_DESTINATION_PREFIX.length());
        if (suffix.contains("/")) {
            return null;
        }

        try {
            return Long.parseLong(suffix);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long extractUserId(Principal principal) {
        if (!(principal instanceof UsernamePasswordAuthenticationToken authentication)) {
            return null;
        }

        return ((AuthUser) authentication.getPrincipal()).getUserId();
    }
}