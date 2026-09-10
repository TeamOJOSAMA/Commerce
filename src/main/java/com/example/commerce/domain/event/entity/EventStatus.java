package com.example.commerce.domain.event.entity;

public enum EventStatus {
    SCHEDULED,   // 시작 전
    ACTIVE,      // 진행중
    ENDED        // 종료 (시간 만료 또는 재고 소진)
}
