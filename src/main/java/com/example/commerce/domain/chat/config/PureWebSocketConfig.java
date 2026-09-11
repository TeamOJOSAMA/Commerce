package com.example.commerce.domain.chat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

// 순수 WebSocket 동작 확인용. STOMP 전환 후 제거 예정
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class PureWebSocketConfig implements WebSocketConfigurer {

    private static final String ECHO_ENDPOINT = "/api/ws-echo";

    private final EchoWebSocketHandler echoWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(echoWebSocketHandler, ECHO_ENDPOINT)
                .setAllowedOriginPatterns("*");
    }
}