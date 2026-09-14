package com.example.commerce.domain.order.dto;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.order.entity.OrderCancelReason;
import com.example.commerce.domain.order.entity.OrderStatus;
import com.example.commerce.domain.refund.entity.RefundStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 입력이 enum뿐인 순수 함수라 도달 가능한 조합을 표로 전부 고정한다.
 * 조합 판정이 클라이언트마다 흩어지지 않게 하려고 만든 값이므로, 규칙이 바뀌면 이 표가 먼저 깨져야 한다.
 */
class OrderDisplayStatusTest {

    static Stream<Arguments> 도달_가능한_조합() {
        return Stream.of(
                // 결제 대기 주문은 결제가 항상 READY다. 실패는 곧바로 주문 취소로 이어진다.
                Arguments.of(OrderStatus.PAYMENT_PENDING, null, null,
                        OrderDisplayStatus.PAYMENT_PENDING),
                // 승인된 주문은 환불 이력으로만 갈린다.
                Arguments.of(OrderStatus.CONFIRMED, null, null,
                        OrderDisplayStatus.PAID),
                Arguments.of(OrderStatus.CONFIRMED, null, RefundStatus.REQUESTED,
                        OrderDisplayStatus.REFUND_REQUESTED),
                // 전액 환불은 주문까지 취소하므로 CONFIRMED로 남은 완료 환불은 부분 환불뿐이다.
                Arguments.of(OrderStatus.CONFIRMED, null, RefundStatus.COMPLETED,
                        OrderDisplayStatus.PARTIALLY_REFUNDED),
                // 취소된 주문은 상태가 하나뿐이라 경위를 사유로 구분한다.
                Arguments.of(OrderStatus.CANCELLED, OrderCancelReason.USER_REQUEST, null,
                        OrderDisplayStatus.CANCELLED),
                Arguments.of(OrderStatus.CANCELLED, OrderCancelReason.PAYMENT_FAILED, null,
                        OrderDisplayStatus.PAYMENT_FAILED),
                Arguments.of(OrderStatus.CANCELLED, OrderCancelReason.REFUND_COMPLETED,
                        RefundStatus.COMPLETED, OrderDisplayStatus.REFUNDED)
        );
    }

    @ParameterizedTest(name = "{0} + {1} + {2} -> {3}")
    @MethodSource("도달_가능한_조합")
    @DisplayName("주문 상태·취소 사유·환불 상태의 도달 가능한 조합을 모두 파생한다")
    void of_mapsEveryReachableCombination(OrderStatus orderStatus,
                                          OrderCancelReason cancelReason,
                                          RefundStatus refundStatus,
                                          OrderDisplayStatus expected) {
        assertThat(OrderDisplayStatus.of(orderStatus, cancelReason, refundStatus))
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("사유 컬럼 도입 전에 취소된 주문은 사유가 없어 일반 취소로 본다")
    void of_treatsMissingCancelReasonAsPlainCancel() {
        assertThat(OrderDisplayStatus.of(OrderStatus.CANCELLED, null, null))
                .isEqualTo(OrderDisplayStatus.CANCELLED);
    }

    @Test
    @DisplayName("결제 대기 주문은 환불 상태가 끼어들어도 결제 대기다")
    void of_ignoresRefundWhilePaymentPending() {
        // 결제 대기 주문에는 환불이 생길 수 없다. 잘못된 데이터가 와도 주문 상태를 우선한다.
        assertThat(OrderDisplayStatus.of(OrderStatus.PAYMENT_PENDING, null, RefundStatus.REQUESTED))
                .isEqualTo(OrderDisplayStatus.PAYMENT_PENDING);
    }

    @Test
    @DisplayName("주문 상태가 없으면 파생할 수 없으므로 예외를 던진다")
    void of_rejectsNullOrderStatus() {
        assertThatThrownBy(() -> OrderDisplayStatus.of(null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
    }
}
