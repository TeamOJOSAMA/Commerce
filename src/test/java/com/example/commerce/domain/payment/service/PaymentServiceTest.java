package com.example.commerce.domain.payment.service;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.order.entity.Order;
import com.example.commerce.domain.order.repository.OrderRepository;
import com.example.commerce.domain.payment.dto.PaymentResponse;
import com.example.commerce.domain.payment.entity.Payment;
import com.example.commerce.domain.payment.entity.PaymentStatus;
import com.example.commerce.domain.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

// mock 저장소로 결제 상태와 응답 변환을 확인하는 테스트이며 실제 동시 트랜잭션을 실행하지 않는다.
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {


    private static final long ORDER_ID = 1L;
    private static final long PAYMENT_AMOUNT = 10_000L;
    private static final long PAYMENT_ID = 100L;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private Order order;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        lenient().when(order.getId()).thenReturn(ORDER_ID);

        lenient().when(order.getPaymentAmount()).thenReturn(PAYMENT_AMOUNT);
    }

    // -------- createPayment --------

    @Test
    @DisplayName("결제 생성 성공 - READY 상태로 저장된다")
    void createPayment_success() {

        given(paymentRepository.existsByOrderId(ORDER_ID)).willReturn(false);
        given(paymentRepository.saveAndFlush(any(Payment.class)))
                .willAnswer(invocation ->  invocation.getArgument(0));

        // 생성 메서드는 내부 도메인 객체를 반환하므로 DTO 대신 저장 대상 Payment의 상태를 확인한다.
        Payment payment = paymentService.createPayment(order, PAYMENT_AMOUNT);

        assertThat(payment.getOrder().getId()).isEqualTo(ORDER_ID);
        assertThat(payment.getAmount()).isEqualTo(PAYMENT_AMOUNT);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
        // READY 생성만으로 결제 완료 시각이 기록되면 안 된다.
        assertThat(payment.getPaidAt()).isNull();
    }

    @Test
    @DisplayName("요청 금액이 주문의 결제 금액과 다르면 AMOUNT_MISMATCH, 저장하지 않는다")
    void createPayment_amountMismatch() {
        assertThatThrownBy(() -> paymentService.createPayment(order, 9_999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AMOUNT_MISMATCH);

        then(paymentRepository).should(never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("동시 요청으로 유니크 제약 위반이 나면 PAYMENT_DUPLICATED 로 변환한다 (2차 방어)")
    void createPayment_duplicate_raceCondition() {

        given(paymentRepository.existsByOrderId(ORDER_ID)).willReturn(false);
        given(paymentRepository.saveAndFlush(any(Payment.class)))
                .willThrow(new DataIntegrityViolationException("uk_payments_order_id"));

        assertThatThrownBy(() -> paymentService.createPayment(order, PAYMENT_AMOUNT))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_DUPLICATED);
    }

    // -------- approvePayment --------

    @Test
    @DisplayName("결제 승인 성공 - PAID + paidAt 기록")
    void approvePayment_success() {
        Payment payment = Payment.of(order, PAYMENT_AMOUNT);

        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));

        PaymentResponse response = paymentService.approvePayment(PAYMENT_ID);

        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.paidAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 승인된 결제를 다시 승인하면 INVALID_PAYMENT_STATUS")
    void approvePayment_alreadyApproved() {
        Payment payment = Payment.of(order, PAYMENT_AMOUNT);
        payment.approve();

        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.approvePayment(PAYMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PAYMENT_STATUS);
    }

    @Test
    @DisplayName("존재하지 않는 결제를 승인하면 PAYMENT_NOT_FOUND")
    void approvePayment_notFound() {
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.approvePayment(PAYMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
    }

    // -------- failPayment --------

    @Test
    @DisplayName("결제 실패 처리 성공 - FAILED + 사유 기록")
    void failPayment_success() {
        Payment payment = Payment.of(order, PAYMENT_AMOUNT);

        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));

        PaymentResponse response = paymentService.failPayment(PAYMENT_ID, "카드 한도 초과");

        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.failReason()).isEqualTo("카드 한도 초과");
    }

    // -------- getPaymentDetail --------

    @Test
    @DisplayName("존재하지 않는 결제를 조회하면 PAYMENT_NOT_FOUND")
    void getPaymentDetail_notFound() {

        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentDetail(PAYMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
    }
}
