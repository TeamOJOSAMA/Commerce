package com.example.commerce.domain.chat.entity;

public enum MessageType {

    TALK,   // 일반 대화
    BOT,    // 자동응답 봇 메시지
    ENTER,  // 입장 시스템 메시지
    LEAVE;  // 퇴장 시스템 메시지

    public boolean isSystemMessage() {
        return this == ENTER || this == LEAVE;
    }
}