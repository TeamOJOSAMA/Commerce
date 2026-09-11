package com.example.commerce.domain.refund.service;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.order.entity.OrderItem;
import com.example.commerce.domain.payment.entity.Payment;
import com.example.commerce.domain.payment.repository.PaymentRepository;
import com.example.commerce.domain.refund.dto.RefundRequest;
import com.example.commerce.domain.refund.dto.RefundResponse;
import com.example.commerce.domain.refund.entity.Refund;
import com.example.commerce.domain.refund.entity.RefundItem;
import com.example.commerce.domain.refund.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public RefundResponse createRefund(RefundRequest refundRequest) {
        Payment payment = paymentRepository.findById(refundRequest.paymentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

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
