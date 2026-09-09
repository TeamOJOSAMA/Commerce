package com.example.commerce.domain.chat.entity;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;

import java.util.Map;
import java.util.Set;

public enum InquiryStatus {

    BOT_HANDLING,   // 봇 응대중
    WAITING,        // 상담원 연결 대기
    IN_PROGRESS,    // 상담원 처리중
    COMPLETED;      // 완료

    // 역방향 전이(COMPLETED -> WAITING 등)를 원천 차단하기 위해 허용 목록으로 관리
    private static final Map<InquiryStatus, Set<InquiryStatus>> ALLOWED_TRANSITIONS = Map.of(
            BOT_HANDLING, Set.of(WAITING),
            WAITING, Set.of(IN_PROGRESS),
            IN_PROGRESS, Set.of(COMPLETED),
            COMPLETED, Set.of()
    );

    public void validateTransitionTo(InquiryStatus target) {
        if (!isTransitableTo(target)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }

    public boolean isTransitableTo(InquiryStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }

    public boolean isClosed() {
        return this == COMPLETED;
    }

    public boolean isBotHandling() {
        return this == BOT_HANDLING;
    }
}