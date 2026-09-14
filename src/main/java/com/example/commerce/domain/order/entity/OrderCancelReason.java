package com.example.commerce.domain.order.entity;

/**
 * 주문이 취소된 경위다. 취소 경로마다 하나씩 대응한다.
 *
 * <p>주문 상태는 {@code CANCELLED} 하나뿐이라 사용자 취소·결제 실패·환불 완료를 구분하지 못한다.
 * 이 값이 그 구분을 맡으므로 취소 경로를 새로 만들면 여기에 값을 먼저 추가한다.</p>
 *
 * <p>{@code message}는 결제를 함께 실패 처리할 때 {@code Payment.failReason}에 남기는 문구다.
 * 컬럼 상한이 50자이므로 그보다 길게 쓰지 않는다.</p>
 */
public enum OrderCancelReason {

    // 결제 대기 주문을 사용자가 직접 취소했다.
    USER_REQUEST("사용자가 주문을 취소했습니다."),
    // 결제 실패로 주문이 취소됐다. 결제 실패 시 자동 취소는 후속 작업이라 지금은 이 값을 쓰는 운영 경로가 없다.
    PAYMENT_FAILED("결제에 실패하여 주문이 취소되었습니다."),
    // 전액 환불이 완료되어 주문 전체가 무효가 됐다. 부분 환불은 주문을 취소하지 않는다.
    REFUND_COMPLETED("환불이 완료되어 주문이 취소되었습니다.");

    private final String message;

    OrderCancelReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}