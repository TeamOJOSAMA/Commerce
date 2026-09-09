package com.example.commerce.domain.chat.config;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.common.jwt.JwtProvider;
import com.example.commerce.domain.auth.entity.AuthUser;
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

import java.util.List;

// HTTP Filter 는 WebSocket 핸드셰이크 이후의 STOMP 프레임을 가로챌 수 없음.
// 따라서 CONNECT 시점에 여기서 JWT 를 검증하고 Principal 을 설정함.
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String ROLE_CLAIM = "role";
    private static final String EMAIL_CLAIM = "email";

    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        // CONNECT 시점에 한 번만 인증함
        // 이후 SEND, SUBSCRIBE 는 인증된 세션이 유지되므로 재검증하지 않음
        accessor.setUser(authenticate(accessor));

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
}