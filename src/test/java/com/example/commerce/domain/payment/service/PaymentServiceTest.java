package com.example.commerce.domain.payment.service;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.coupon.service.CouponService;
import com.example.commerce.domain.order.entity.Order;
import com.example.commerce.domain.order.entity.OrderStatus;
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

    private static final long USER_ID = 1L;
    private static final long ORDER_ID = 1L;
    private static final long PAYMENT_AMOUNT = 10_000L;
    private static final long PAYMENT_ID = 100L;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CouponService couponService;

    @Mock
    private Order order;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        lenient().when(order.getId()).thenReturn(ORDER_ID);
        lenient().when(order.getUserId()).thenReturn(USER_ID);
        lenient().when(order.getPaymentAmount()).thenReturn(PAYMENT_AMOUNT);
        lenient().when(order.getStatus()).thenReturn(OrderStatus.PAYMENT_PENDING);
    }

    // -------- createPayment --------

    @Test
    @DisplayName("결제 생성 성공 - READY 상태로 저장된다")
    void createPayment_success() {

        given(orderRepository.findByIdForUpdate(ORDER_ID))
                .willReturn(Optional.of(order));

        given(paymentRepository.existsByOrderId(ORDER_ID))
                .willReturn(false);

        given(paymentRepository.save(any(Payment.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.createPayment(order, PAYMENT_AMOUNT);

        assertThat(payment.getOrder().getId()).isEqualTo(ORDER_ID);
        assertThat(payment.getAmount()).isEqualTo(PAYMENT_AMOUNT);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
        assertThat(payment.getPaidAt()).isNull();
    }

    @Test
    @DisplayName("요청 금액이 주문의 결제 금액과 다르면 AMOUNT_MISMATCH, 저장하지 않는다")
    void createPayment_amountMismatch() {

        given(orderRepository.findByIdForUpdate(ORDER_ID))
                .willReturn(Optional.of(order));

        given(paymentRepository.existsByOrderId(ORDER_ID))
                .willReturn(false);

        assertThatThrownBy(() -> paymentService.createPayment(order, 9_999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AMOUNT_MISMATCH);

        then(paymentRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("이미 결제가 존재하는 주문이면 PAYMENT_DUPLICATED")
    void createPayment_duplicated() {

        given(orderRepository.findByIdForUpdate(ORDER_ID))
                .willReturn(Optional.of(order));

        given(paymentRepository.existsByOrderId(ORDER_ID))
                .willReturn(true);

        assertThatThrownBy(() -> paymentService.createPayment(order, PAYMENT_AMOUNT))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_DUPLICATED);

        then(paymentRepository).should(never()).save(any());
    }

    // -------- approvePayment --------

    @Test
    @DisplayName("결제 승인 성공 - PAID + paidAt 기록")
    void approvePayment_success() {

        Payment payment = Payment.of(order, PAYMENT_AMOUNT);

        given(paymentRepository.findOrderIdByPaymentId(PAYMENT_ID))
                .willReturn(Optional.of(ORDER_ID));

        given(orderRepository.findByIdForUpdate(ORDER_ID))
                .willReturn(Optional.of(order));

        given(paymentRepository.findByIdForUpdate(PAYMENT_ID))
                .willReturn(Optional.of(payment));

        PaymentResponse response =
                paymentService.approvePayment(USER_ID, PAYMENT_ID);

        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.paidAt()).isNotNull();

        then(order).should().confirm();
    }

    @Test
    @DisplayName("이미 승인된 결제를 다시 승인하면 INVALID_PAYMENT_STATUS")
    void approvePayment_alreadyApproved() {

        Payment payment = Payment.of(order, PAYMENT_AMOUNT);
        payment.approve();

        given(paymentRepository.findOrderIdByPaymentId(PAYMENT_ID))
                .willReturn(Optional.of(ORDER_ID));

        given(orderRepository.findByIdForUpdate(ORDER_ID))
                .willReturn(Optional.of(order));

        given(paymentRepository.findByIdForUpdate(PAYMENT_ID))
                .willReturn(Optional.of(payment));

        assertThatThrownBy(() ->
                paymentService.approvePayment(USER_ID, PAYMENT_ID)
        )
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PAYMENT_STATUS);
    }

    @Test
    @DisplayName("존재하지 않는 결제를 승인하면 PAYMENT_NOT_FOUND")
    void approvePayment_notFound() {

        given(paymentRepository.findOrderIdByPaymentId(PAYMENT_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                paymentService.approvePayment(USER_ID, PAYMENT_ID)
        )
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
    }

    // -------- failPayment --------

    @Test
    @DisplayName("결제 실패 처리 성공 - FAILED + 사유 기록")
    void failPayment_success() {

        Payment payment = Payment.of(order, PAYMENT_AMOUNT);

        given(paymentRepository.findOrderIdByPaymentId(PAYMENT_ID))
                .willReturn(Optional.of(ORDER_ID));

        given(orderRepository.findByIdForUpdate(ORDER_ID))
                .willReturn(Optional.of(order));

        given(paymentRepository.findByIdForUpdate(PAYMENT_ID))
                .willReturn(Optional.of(payment));

        PaymentResponse response =
                paymentService.failPayment(
                        USER_ID,
                        PAYMENT_ID,
                        "카드 한도 초과"
                );

        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.failReason()).isEqualTo("카드 한도 초과");
    }

    // -------- getPaymentDetail --------

    @Test
    @DisplayName("존재하지 않는 결제를 조회하면 PAYMENT_NOT_FOUND")
    void getPaymentDetail_notFound() {

        given(paymentRepository.findById(PAYMENT_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                paymentService.getPaymentDetail(USER_ID, PAYMENT_ID)
        )
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
    }
}