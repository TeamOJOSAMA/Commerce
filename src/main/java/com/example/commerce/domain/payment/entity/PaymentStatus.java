package com.example.commerce.domain.payment.entity;

public enum PaymentStatus {
    READY,      // 결제 요청 생성 (승인/실패 대기)
    PAID,       // 결제 승인 완료
    FAILED,     // 결제 승인 실패
    CANCELED    // 결제 취소 (환불 처리 시작 시점)
}
