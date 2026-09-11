package com.example.commerce.domain.chat.config;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.common.jwt.JwtProvider;
import com.example.commerce.domain.auth.entity.AuthUser;
import com.example.commerce.domain.chat.repository.ChatRoomRepository;
import com.example.commerce.domain.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

// HTTP Filter 는 WebSocket 핸드셰이크 이후의 STOMP 프레임을 가로챌 수 없다.
// CONNECT 시점에 JWT 를 검증하고, SUBSCRIBE 시점에 채팅방 접근 권한을 확인한다
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String ROLE_CLAIM = "role";
    private static final String EMAIL_CLAIM = "email";
    private static final String ROOM_DESTINATION_PREFIX = "/sub/chat-rooms/";
    private static final String ADMIN_DESTINATION = "/sub/chat-rooms/admin";

    private final JwtProvider jwtProvider;
    private final ChatRoomRepository chatRoomRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // CONNECT 시점에 한 번만 인증한다.
            // 이후 SEND 는 인증된 세션이 유지되므로 재검증하지 않는다
            accessor.setUser(authenticate(accessor));

            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            // 검증하지 않으면 임의의 방을 구독해 타인의 대화를 엿볼 수 있다
            validateSubscribable(accessor);
        }

        return message;
    }

    private UsernamePasswordAuthenticationToken authenticate(StompHeaderAccessor accessor) {
        String bearerToken = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);

        if (bearerToken == null) {
            throw new BusinessException(ErrorCode.WEBSOCKET_UNAUTHORIZED);
        }

        try {
            String token = jwtProvider.substringToken(bearerToken);
            Claims claims = jwtProvider.extractClaims(token);

            UserRole role = UserRole.valueOf(claims.get(ROLE_CLAIM, String.class));
            AuthUser authUser = new AuthUser(
                    Long.parseLong(claims.getSubject()),
                    claims.get(EMAIL_CLAIM, String.class),
                    role
            );

            List<GrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role.name()));

            return new UsernamePasswordAuthenticationToken(authUser, null, authorities);
        } catch (Exception e) {
            log.error("STOMP CONNECT 인증 실패", e);

            throw new BusinessException(ErrorCode.WEBSOCKET_UNAUTHORIZED);
        }
    }

    private void validateSubscribable(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(ROOM_DESTINATION_PREFIX)) {
            return;
        }

        AuthUser authUser = extractAuthUser(accessor.getUser());
        if (authUser == null) {
            throw new BusinessException(ErrorCode.WEBSOCKET_UNAUTHORIZED);
        }

        boolean isAdmin = authUser.getRole() == UserRole.ADMIN;

        // 관리자 대시보드는 ADMIN 만 구독할 수 있다
        if (destination.startsWith(ADMIN_DESTINATION)) {
            if (!isAdmin) {
                throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
            }

            return;
        }

        Long chatRoomId = extractChatRoomId(destination);
        if (chatRoomId == null) {
            return;
        }

        if (!chatRoomRepository.existsAccessibleBy(chatRoomId, authUser.getUserId(), isAdmin)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }

    // /sub/chat-rooms/1, /sub/chat-rooms/1/errors 모두에서 방 번호를 추출한다
    private Long extractChatRoomId(String destination) {
        String suffix = destination.substring(ROOM_DESTINATION_PREFIX.length());
        int separatorIndex = suffix.indexOf('/');
        String roomId = (separatorIndex == -1) ? suffix : suffix.substring(0, separatorIndex);

        try {
            return Long.parseLong(roomId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private AuthUser extractAuthUser(Principal principal) {
        if (!(principal instanceof UsernamePasswordAuthenticationToken authentication)) {
            return null;
        }

        return (AuthUser) authentication.getPrincipal();
    }
}