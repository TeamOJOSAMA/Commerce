package com.example.commerce.domain.payment.entity;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.order.entity.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PaymentTest {

    private static final long AMOUNT = 10_000L;

    private final Order order = mock(Order.class);

    @Test
    @DisplayName("of() 로 생성하면 상태는 READY, paidAt.failReason 은 비어 있다")
    void of_createsReadyPayment() {
        Payment payment = Payment.of(order, AMOUNT);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
        assertThat(payment.getAmount()).isEqualTo(AMOUNT);
        assertThat(payment.getPaidAt()).isNull();
        assertThat(payment.getFailReason()).isNull();
        assertThat(payment.isPaid()).isFalse();
    }

    @Test
    @DisplayName("approve() 는 READY 결제를 PAID 로 전이시키고 paidAt 을 채운다")
    void approve_movesTopaid() {
        Payment payment = Payment.of(order, AMOUNT);

        payment.approve();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(payment.isPaid()).isTrue();
    }

    @Test
    @DisplayName("이미 승인된 결제를 다시 approve() 하면 INVALID_PAYMENT_STATUS 예외")
    void approve_twice_throws() {
        Payment payment = Payment.of(order, AMOUNT);
        payment.approve();

        assertThatThrownBy(payment::approve)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException)e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PAYMENT_STATUS);
    }

    @Test
    @DisplayName("fail() 은 READY 결제를 FAILED 로 전이시키고 사유를 기록한다")
    void fail_movesToFailed() {
        Payment payment = Payment.of(order, AMOUNT);

        payment.fail("승인 거절");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailReason()).isEqualTo("승인 거절");
    }

    @Test
    @DisplayName("승인된 결제에 fail() 을 호출하면 INVALID_PAYMENT_STATUS 예외")
    void fail_afterApprove_throws() {
        Payment payment = Payment.of(order, AMOUNT);
        payment.approve();

        assertThatThrownBy(() -> payment.fail("사유"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException)e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PAYMENT_STATUS);
    }

    @Test
    @DisplayName("cancel() 은 PAID 결제를 CANCELED 로 전이시킨다")
    void cancel_afterApprove_movesToCancelled() {
        Payment payment = Payment.of(order, AMOUNT);
        payment.approve();

        payment.cancel();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
    }

    @Test
    @DisplayName("READY 결제에 cancel() 을 호출하면 INVALID_PAYMENT_STATUS 예외")
    void cancel_whenReady_throws() {
        Payment payment = Payment.of(order, AMOUNT);

        assertThatThrownBy(payment::cancel)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException)e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PAYMENT_STATUS);
    }
}
