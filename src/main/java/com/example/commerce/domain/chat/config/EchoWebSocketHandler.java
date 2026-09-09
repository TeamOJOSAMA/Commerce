package com.example.commerce.domain.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

// 순수 WebSocket 동작 확인용. STOMP 전환 후 제거 예정
@Slf4j
@Component
public class EchoWebSocketHandler extends TextWebSocketHandler {

    // 순수 WebSocket 은 브로커가 없어 세션을 직접 들고 있어야 한다
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("연결됨: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("수신: {}", message.getPayload());

        // 구독 개념이 없어 모든 세션에 직접 전송해야 한다
        for (WebSocketSession connectedSession : sessions) {
            if (connectedSession.isOpen()) {
                connectedSession.sendMessage(new TextMessage(message.getPayload()));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("연결 종료: {}", session.getId());
    }
}