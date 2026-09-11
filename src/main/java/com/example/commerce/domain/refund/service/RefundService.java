package com.example.commerce.domain.refund.service;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.order.entity.Order;
import com.example.commerce.domain.order.entity.OrderItem;
import com.example.commerce.domain.order.repository.OrderRepository;
import com.example.commerce.domain.payment.entity.Payment;
import com.example.commerce.domain.payment.repository.PaymentRepository;
import com.example.commerce.domain.product.entity.Product;
import com.example.commerce.domain.product.repository.ProductRepository;
import com.example.commerce.domain.refund.dto.RefundRequest;
import com.example.commerce.domain.refund.dto.RefundResponse;
import com.example.commerce.domain.refund.entity.Refund;
import com.example.commerce.domain.refund.entity.RefundItem;
import com.example.commerce.domain.refund.entity.RefundType;
import com.example.commerce.domain.refund.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public RefundResponse completeRefund(Long userId, Long refundId) {
        Refund refund = refundRepository.findByIdForUpdate(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND));

        validateOwner(refund.getPayment(), userId);

        refund.complete();
        refund.getPayment().cancel();

        // 전액 환불이면 주문 전체가 무효 그래서 주문도 취소
        // 부분 환불은 남은 항목이 유효한 주문으로 남기때문에 주문 상태는 건드리지 않음
        if (refund.getRefundType() == RefundType.FULL) {
            cancelOrder(refund.getPayment().getOrder().getId());
        }

        restoreStock(refund);

        log.info("환불 완료 - refundId: {}, paymentId: {}, type: {}", refundId, refund.getPayment().getId(), refund.getRefundType());
        return RefundResponse.from(refund);
    }

    private void cancelOrder(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        order.cancel();
    }

    private void restoreStock(Refund refund) {
        Map<Long, Integer> quantityByProductId = refund.getRefundItems().stream()
                .collect(Collectors.toMap(
                        refundItem -> refundItem.getOrderItem().getProductId(),
                        RefundItem::getQuantity,
                        Integer::sum));

        List<Product> products = productRepository.findAllByIdsForUpdate(
                new ArrayList<>(quantityByProductId.keySet())
        );
        products.forEach(product -> product.restoreStock(quantityByProductId.get(product.getId())));
    }

    @Transactional
    public RefundResponse createRefund(Long userId, RefundRequest refundRequest) {
        Payment payment = paymentRepository.findByIdForUpdate(refundRequest.paymentId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        validateOwner(payment, userId);
        validatePaid(payment);

        if (refundRepository.existsByPaymentId(payment.getId())) {
            throw new BusinessException(ErrorCode.REFUND_NOT_ALLOWED, "이미 환불이 접수된 결제입니다.");
        }

        List<OrderItem> orderItems = payment.getOrder().getOrderItems();
        List<RefundItem> refundItems = buildRefundItems(refundRequest, orderItems);

        Refund refund = new Refund(payment, refundItems, refundRequest.reason(), refundRequest.refundType());

        try {
            Refund saved = refundRepository.saveAndFlush(refund);
            log.info("환불 요청 생성 - refundId: {}, paymentId: {}, type: {}", saved.getId(), payment.getId(), refundRequest.refundType());
            return  RefundResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.REFUND_NOT_ALLOWED, "이미 환불이 접수된 결제입니다.");
        }
    }

    private void validateOwner(Payment payment, Long userId) {
        if (userId == null || !userId.equals(payment.getOrder().getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }
    }

    private void validatePaid(Payment payment) {
        if (!payment.isPaid()) {
            throw new BusinessException(ErrorCode.REFUND_NOT_ALLOWED, "결제가 완료된 건만 환불할 수 있습니다.");
        }
    }

    private List<RefundItem> buildRefundItems(RefundRequest refundRequest, List<OrderItem> orderItems) {
        return switch (refundRequest.refundType()) {

            case FULL -> orderItems.stream()
                    .map(orderItem -> new RefundItem(orderItem, orderItem.getQuantity()))
                    .toList();

            case PARTIAL -> {
                Map<Long, OrderItem> orderItemMAP = orderItems.stream()
                        .collect(Collectors.toMap(OrderItem::getId, Function.identity()));

                yield refundRequest.items().stream()
                        .map(item -> {
                            OrderItem orderItem = orderItemMAP.get(item.orderItemId());

                            if (orderItem ==null) {
                                throw new BusinessException(ErrorCode.REFUND_ITEM_NOT_FOUND);
                            }
                            return new RefundItem(orderItem, item.quantity());
                        })
                        .toList();
            }
        };
    }
}
