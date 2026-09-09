package com.example.commerce.domain.chat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String STOMP_ENDPOINT = "/ws-stomp";
    private static final String SUBSCRIBE_PREFIX = "/sub";
    private static final String PUBLISH_PREFIX = "/pub";
    private static final String USER_PREFIX = "/user";

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(STOMP_ENDPOINT)
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트가 구독하는 경로. 내장 브로커가 이 경로로 메시지를 전달
        registry.enableSimpleBroker(SUBSCRIBE_PREFIX, USER_PREFIX);

        // 클라이언트가 서버로 발행할 때 붙이는 경로. @MessageMapping 으로 라우팅됨
        registry.setApplicationDestinationPrefixes(PUBLISH_PREFIX);

        // 특정 사용자에게만 보낼 때 사용하는 prefix
        registry.setUserDestinationPrefix(USER_PREFIX);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}