package com.example.commerce.domain.order.dto;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.order.entity.OrderCancelReason;
import com.example.commerce.domain.order.entity.OrderStatus;
import com.example.commerce.domain.refund.entity.RefundStatus;

/**
 * 주문 화면이 보여 줄 하나의 진행 상태다. 주문 상태·취소 사유·환불 상태를 합쳐 파생한다.
 *
 * <p>이 값을 두는 이유는 조합 판정이 클라이언트마다 흩어지는 것을 막기 위해서다. 화면은
 * {@code OrderStatus}, 결제 상태, 환불 상태를 각각 받아 조합해야 했고 그 규칙은 서버만 알고 있다.</p>
 *
 * <p><b>결제 상태는 입력이 아니다.</b> 결제 실패는 곧 주문 취소이고(PaymentFacade), 취소 경위는
 * {@link OrderCancelReason}이 들고 있으므로 {@code PaymentStatus}로만 알 수 있는 것이 남지 않는다.
 * 결제 금액·승인 시각 같은 결제 자체의 내역은 {@code GET /api/payments/{paymentId}}가 답한다.</p>
 *
 * <p>한 결제에 여러 건의 부분 환불이 쌓일 수 있어({@link com.example.commerce.domain.refund.service.RefundService})
 * {@code refundStatus}는 그중 최신 한 건의 상태만 받는다. 그래서 {@code PARTIALLY_REFUNDED}는
 * "완료된 환불이 최소 한 건 있다"는 뜻일 뿐, "더 이상 환불할 항목이 없다"는 뜻이 아니다. 항목별로
 * 아직 환불 가능한 수량이 남아 있는지는 {@link OrderItemResponse#refundedQuantity()}로 화면이 직접 계산한다.</p>
 */
public enum OrderDisplayStatus {

    // 결제 대기. 결제는 항상 READY다(실패는 곧바로 주문 취소로 이어진다).
    PAYMENT_PENDING,
    // 결제 완료. 환불 이력이 없다.
    PAID,
    // 환불이 접수되어 처리를 기다린다. 주문과 결제는 아직 그대로다.
    REFUND_REQUESTED,
    // 부분 환불이 완료됐고 남은 항목은 유효하다. 전액 환불이면 주문이 취소되므로 여기로 오지 않는다.
    PARTIALLY_REFUNDED,
    // 전액 환불이 완료되어 주문이 취소됐다.
    REFUNDED,
    // 결제에 실패해 주문이 자동 취소됐다.
    PAYMENT_FAILED,
    // 사용자가 결제 대기 주문을 취소했다. 사유를 모르는 기존 취소 건도 여기로 떨어진다.
    CANCELLED;

    /**
     * 입력이 모두 enum인 순수 함수다. 도달 가능한 조합이 일곱 가지뿐이라 표 테스트로 전부 고정할 수 있다.
     *
     * @param cancelReason 취소된 주문만 값을 갖는다. 기존 행은 null이라 사유 미상으로 다룬다.
     * @param refundStatus 환불 이력이 없으면 null이다.
     */
    public static OrderDisplayStatus of(OrderStatus orderStatus,
                                        OrderCancelReason cancelReason,
                                        RefundStatus refundStatus) {
        if (orderStatus == null) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS, "주문 상태는 필수입니다.");
        }

        return switch (orderStatus) {
            case PAYMENT_PENDING -> PAYMENT_PENDING;
            case CONFIRMED -> ofConfirmed(refundStatus);
            case CANCELLED -> ofCancelled(cancelReason);
        };
    }

    // 승인된 주문은 환불 진행 정도로만 갈린다. 결제는 언제나 PAID다.
    private static OrderDisplayStatus ofConfirmed(RefundStatus refundStatus) {
        if (refundStatus == null) {
            return PAID;
        }

        return switch (refundStatus) {
            case REQUESTED -> REFUND_REQUESTED;
            // 전액 환불은 주문까지 취소하므로 CONFIRMED로 남은 완료 환불은 부분 환불뿐이다.
            case COMPLETED -> PARTIALLY_REFUNDED;
        };
    }

    // 취소된 주문은 상태가 하나뿐이라 경위를 사유로만 구분할 수 있다.
    private static OrderDisplayStatus ofCancelled(OrderCancelReason cancelReason) {
        if (cancelReason == null) {
            // 사유 컬럼 도입 전에 취소된 주문이다. 사용자 취소와 구분할 근거가 없어 일반 취소로 본다.
            return CANCELLED;
        }

        return switch (cancelReason) {
            case USER_REQUEST -> CANCELLED;
            case PAYMENT_FAILED -> PAYMENT_FAILED;
            case REFUND_COMPLETED -> REFUNDED;
        };
    }
}
