package com.example.commerce.domain.payment.service;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.coupon.service.CouponService;
import com.example.commerce.domain.order.entity.Order;
import com.example.commerce.domain.order.entity.OrderStatus;
import com.example.commerce.domain.order.repository.OrderRepository;
import com.example.commerce.domain.payment.dto.PaymentResponse;
import com.example.commerce.domain.payment.entity.Payment;
import com.example.commerce.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 생성·승인·실패를 처리하고 승인 시 주문 확정과 쿠폰 사용을 함께 반영한다.
 * 상태 변경은 주문 → 결제 → 쿠폰 순서로 잠가 주문 취소와 같은 순서를 유지한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CouponService couponService;

    /** 주문 생성 트랜잭션에 참여하여 저장된 주문에 READY 결제를 한 건 생성한다. */
    @Transactional
    public Payment createPayment(Order order, long amount) {
        // 결제가 주문을 참조하므로 Facade에서 주문을 먼저 저장해 ID를 확보해야 한다.
        if (order == null || order.getId() == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        // 기존 주문에 대한 호출도 취소와 겹치지 않도록 주문 잠금과 상태 검사를 수행한다.
        Order lockedOrder = getOrderForUpdate(order.getId());

        validatePendingOrder(lockedOrder);
        // 하나의 주문에 대한 결제 요청은 한 번만 생성 가능 (중복 결제 요청 방지)
        validateDuplicatePayment(lockedOrder.getId());
        // 내부 호출에서 전달된 금액도 저장된 주문의 최종 결제 금액과 비교한다.
        validatePaymentAmount(lockedOrder, amount);

        Payment payment = Payment.of(lockedOrder, amount);
        Payment savedPayment = paymentRepository.save(payment);

        log.info("결제 요청 생성 완료 - orderId: {}, amount: {}", order.getId(), amount);
        // 내부 호출자인 OrderFacade가 주문과 결제를 묶어 응답 DTO로 변환한다.
        return savedPayment;
    }

    // 빠른 중복 검사이며 최종적인 주문당 한 건 보장은 DB 유일성 제약도 담당한다.
    private void validateDuplicatePayment(Long orderId) {
        if (paymentRepository.existsByOrderId(orderId)) {
            throw new BusinessException(ErrorCode.PAYMENT_DUPLICATED);
        }
    }

    /** 결제 승인·쿠폰 사용·주문 확정을 한 트랜잭션으로 묶어 일부 상태만 남지 않게 한다. */
    @Transactional
    public PaymentResponse approvePayment(Long paymentId) {
        // 주문을 먼저 잠가 취소 완료 후 들어온 승인을 대기 상태 검사에서 거부한다.
        Order order = getPaymentOrderForUpdate(paymentId);
        validatePendingOrder(order);
        Payment payment = getPaymentForUpdate(paymentId);

        // 잠금으로 동시 처리를 직렬화하고, 엔티티에서 READY 상태인지 다시 검증한다.
        payment.approve();
        if (order.getUserCouponId() != null) {
            // 주문이 예약한 쿠폰을 USED로 확정한다. 실패하면 위 결제 승인도 롤백된다.
            couponService.useCoupon(order.getUserId(), order.getUserCouponId());
        }
        // 결제와 주문이 각각 PAID, CONFIRMED로 함께 커밋되도록 한다.
        order.confirm();

        log.info("결제 승인 완료 - paymentId: {}", paymentId);
        return PaymentResponse.from(payment);
    }

    /** 결제 실패만 기록한다. 대기 주문과 쿠폰 예약은 주문 취소 시 별도로 정리한다. */
    @Transactional
    public PaymentResponse failPayment(Long paymentId, String failReason) {
        Order order = getPaymentOrderForUpdate(paymentId);
        validatePendingOrder(order);
        Payment payment = getPaymentForUpdate(paymentId);
        payment.fail(failReason);

        log.info("결제 실패 처리 완료 - paymentId: {}, reason: {}", paymentId, failReason);
        return PaymentResponse.from(payment);
    }

    // 상태를 변경하지 않는 상세 조회는 클래스의 읽기 전용 트랜잭션을 사용한다.
    public PaymentResponse getPaymentDetail(Long paymentId) {
        return PaymentResponse.from(getPayment(paymentId));
    }

    // 조회에만 사용한다. 승인·실패에는 잠금 조회 메서드를 사용해야 한다.
    private Payment getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    // 반환 이후에도 호출 트랜잭션이 끝날 때까지 주문 잠금을 유지한다.
    private Order getOrderForUpdate(Long orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    private Order getPaymentOrderForUpdate(Long paymentId) {
        // 잠금 대기 전 결제 엔티티를 로딩하지 않고 주문 ID만 조회한다.
        // 대기 중 바뀐 결제 상태 대신 영속성 컨텍스트의 이전 객체를 읽는 일을 피한다.
        Long orderId = paymentRepository.findOrderIdByPaymentId(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        return getOrderForUpdate(orderId);
    }

    // 반드시 주문 잠금 이후 호출하여 승인·실패·취소의 잠금 순서를 맞춘다.
    private Payment getPaymentForUpdate(Long paymentId) {
        return paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    // 취소되었거나 이미 확정된 주문에는 결제 생성·승인·실패를 진행하지 않는다.
    private void validatePendingOrder(Order order) {
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
    }

    // 할인 전 총액이 아니라 쿠폰 할인이 반영된 최종 결제 금액과 비교한다.
    private void validatePaymentAmount(Order order, Long requestAmount) {
        if (!order.getPaymentAmount().equals(requestAmount)) {
            throw new BusinessException(ErrorCode.AMOUNT_MISMATCH);
        }
    }
}
