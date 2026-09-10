package com.example.commerce.domain.payment.service;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.order.entity.Order;
import com.example.commerce.domain.order.repository.OrderRepository;
import com.example.commerce.domain.payment.dto.PaymentResponse;
import com.example.commerce.domain.payment.entity.Payment;
import com.example.commerce.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public PaymentResponse createPayment(Order order, long amount) {
        // 하나의 주문에 대한 결제 요청은 한 번만 생성 가능 (중복 결제 요청 방지)
        validateDuplicatePayment(order.getId());

        // 클라이언트 요청 금액과 실제 주문 결제 금액 일치 검증
        validatePaymentAmount(order, amount);

        Payment payment = Payment.of(order, amount);

        Payment savedPayment;
        try {
            savedPayment = paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PAYMENT_DUPLICATED);
        }

        log.info("결제 요청 생성 완료 - orderId: {}, amount: {}", order.getId(), amount);
        return PaymentResponse.from(savedPayment);
    }

    private void validateDuplicatePayment(Long orderId) {
        if (paymentRepository.existsByOrderId(orderId)) {
            throw new BusinessException(ErrorCode.PAYMENT_DUPLICATED);
        }
    }

    @Transactional
    public PaymentResponse approvePayment(Long paymentId) {
        Payment payment = getPayment(paymentId);

        // 모의(mock) 결제 승인: 엔티티 내부에서 READY 상태 검증 -> 중복 승인 자동 차단
        payment.approve();

        log.info("결제 승인 완료 - paymentId: {}", paymentId);
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse failPayment(Long paymentId, String failReason) {
        Payment payment = getPayment(paymentId);
        payment.fail(failReason);

        log.info("결제 실패 처리 완료 - paymentId: {}, reason: {}", paymentId, failReason);
        return PaymentResponse.from(payment);
    }

    public PaymentResponse getPaymentDetail(Long paymentId) {
        return PaymentResponse.from(getPayment(paymentId));
    }

    private Payment getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    private Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    private void validatePaymentAmount(Order order, Long requestAmount) {
        if (!order.getPaymentAmount().equals(requestAmount)) {
            throw new BusinessException(ErrorCode.AMOUNT_MISMATCH);
        }
    }
}
