package com.example.commerce.domain.refund.service;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.order.entity.Order;
import com.example.commerce.domain.order.entity.OrderCancelReason;
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
import com.example.commerce.domain.refund.repository.RefundItemRepository;
import com.example.commerce.domain.refund.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundService {

    private final RefundRepository refundRepository;
    private final RefundItemRepository refundItemRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    // 주문 상세 조립을 위한 내부 조회다. 한 결제에 여러 건의 부분 환불이 있을 수 있어
    // 최신 한 건만 대표로 돌려준다. 환불이 없을 수 있어 Optional로 반환한다.
    // 소유권은 주문을 조회한 호출자가 이미 검증한다.
    public Optional<Refund> findRefundByPaymentId(Long paymentId) {
        if (paymentId == null) {
            return Optional.empty();
        }

        return refundRepository.findFirstByPaymentIdOrderByIdDesc(paymentId);
    }

    // 주문 목록 조립용 내부 조회다. 결제 ID를 키로 최신 환불 한 건씩만 돌려주므로 호출자가 주문과 이어 붙인다.
    public Map<Long, Refund> findRefundsByPaymentIds(List<Long> paymentIds) {
        if (paymentIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Refund> latestRefundByPaymentId = new LinkedHashMap<>();
        // id 내림차순이라 결제별로 처음 만나는 항목이 최신 환불이다.
        for (Refund refund : refundRepository.findAllByPaymentIdInOrderByIdDesc(paymentIds)) {
            latestRefundByPaymentId.putIfAbsent(refund.getPayment().getId(), refund);
        }
        return latestRefundByPaymentId;
    }

    // 주문 상세 조립용 내부 조회다. 주문 항목별로 지금까지 걸린(요청+완료 불문) 환불 수량을 돌려줘서,
    // 화면이 "얼마나 더 환불할 수 있는지"를 계산할 수 있게 한다. createRefund()의 잔여 수량 검증과
    // 같은 기준(getAlreadyRefundedQuantityByOrderItemId)을 쓴다.
    public Map<Long, Integer> getRefundedQuantityByOrderItemIds(List<Long> orderItemIds) {
        if (orderItemIds.isEmpty()) {
            return Map.of();
        }

        return refundItemRepository.findAllByOrderItem_IdIn(orderItemIds).stream()
                .collect(Collectors.toMap(
                        refundItem -> refundItem.getOrderItem().getId(),
                        RefundItem::getQuantity,
                        Integer::sum));
    }

    @Transactional
    public RefundResponse completeRefund(Long userId, Long refundId) {
        Refund refund = refundRepository.findByIdForUpdate(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND));

        validateOwner(refund.getPayment(), userId);

        refund.complete();

        // 전액 환불이면 결제·주문이 전부 무효라 결제도 취소하고 주문도 취소한다.
        // 부분 환불은 결제·주문 모두 유효하게 남아야, 남은 수량을 나중에 또 부분 환불할 수 있다.
        // (예전엔 부분 환불도 결제를 CANCELED로 만들어 버려서, 두 번째 부분 환불이 validatePaid()에
        // 걸려 항상 거부됐다 — 한 결제에 부분 환불을 여러 번 허용하면서 함께 고쳤다.)
        if (refund.getRefundType() == RefundType.FULL) {
            refund.getPayment().cancel();
            cancelOrder(refund.getPayment().getOrder().getId());
        }

        restoreStock(refund);

        log.info("환불 완료 - refundId: {}, paymentId: {}, type: {}", refundId, refund.getPayment().getId(), refund.getRefundType());
        return RefundResponse.from(refund);
    }

    private void cancelOrder(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        order.cancel(OrderCancelReason.REFUND_COMPLETED);
    }

    private void restoreStock(Refund refund) {
        Map<Long, Integer> quantityByProductId = refund.getRefundItems().stream()
                .collect(Collectors.toMap(
                        refundItem -> refundItem.getOrderItem().getProductId(),
                        RefundItem::getQuantity,
                        Integer::sum));

        List<Product> products = productRepository.findAllByIdsForUpdate(
                new ArrayList<>(quantityByProductId.keySet()));

        for (Product product : products) {
            Integer quantity = quantityByProductId.get(product.getId());

            if ((long) product.getStock() + quantity > Integer.MAX_VALUE) {
                throw new BusinessException(ErrorCode.STOCK_AMOUNT_OVERFLOW);
            }

            product.restoreStock(quantity);
        }
    }

    @Transactional
    public RefundResponse createRefund(Long userId, RefundRequest refundRequest) {
        // 같은 결제에 대한 동시 환불 요청은 이 잠금으로 직렬화되므로, 아래 잔여 수량 검증이
        // 경쟁 상태 없이 안전하게 이뤄진다(그 사이 다른 요청은 커밋 전까지 대기한다).
        Payment payment = paymentRepository.findByIdForUpdate(refundRequest.paymentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        validateOwner(payment, userId);
        validatePaid(payment);

        List<OrderItem> orderItems = payment.getOrder().getOrderItems();
        Map<Long, Integer> alreadyRefundedQuantityByOrderItemId =
                getRefundedQuantityByOrderItemIds(orderItems.stream().map(OrderItem::getId).toList());
        List<RefundItem> refundItems = buildRefundItems(refundRequest, orderItems, alreadyRefundedQuantityByOrderItemId);

        Refund refund = new Refund(payment, refundItems, refundRequest.reason(), refundRequest.refundType());

        Refund saved = refundRepository.saveAndFlush(refund);
        log.info("환불 요청 생성 - refundId: {}, paymentId: {}, type: {}", saved.getId(), payment.getId(), refundRequest.refundType());
        return RefundResponse.from(saved);
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

    private List<RefundItem> buildRefundItems(RefundRequest refundRequest, List<OrderItem> orderItems,
                                               Map<Long, Integer> alreadyRefundedQuantityByOrderItemId) {
        return switch (refundRequest.refundType()) {

            // 전액 환불 = 항목별로 "아직 안 걸린" 나머지 수량을 전부 환불한다. 이미 부분 환불로
            // 다 소진된 항목은 건너뛰고, 남은 게 하나도 없으면 환불할 게 없다는 의미로 거부한다.
            case FULL -> {
                List<RefundItem> items = orderItems.stream()
                        .map(orderItem -> {
                            int remaining = remainingQuantity(orderItem, alreadyRefundedQuantityByOrderItemId);
                            return remaining <= 0 ? null : new RefundItem(orderItem, remaining);
                        })
                        .filter(Objects::nonNull)
                        .toList();

                if (items.isEmpty()) {
                    throw new BusinessException(ErrorCode.REFUND_NOT_ALLOWED, "이미 전액 환불 처리된 결제입니다.");
                }
                yield items;
            }

            case PARTIAL -> {
                Map<Long, OrderItem> orderItemMap = orderItems.stream()
                        .collect(Collectors.toMap(OrderItem::getId, Function.identity()));

                yield refundRequest.items().stream()
                        .map(item -> {
                            OrderItem orderItem = orderItemMap.get(item.orderItemId());

                            if (orderItem == null) {
                                throw new BusinessException(ErrorCode.REFUND_ITEM_NOT_FOUND);
                            }

                            int remaining = remainingQuantity(orderItem, alreadyRefundedQuantityByOrderItemId);
                            if (item.quantity() > remaining) {
                                throw new BusinessException(ErrorCode.REFUND_ITEM_NOT_FOUND,
                                        "환불 가능한 수량(%d개)을 초과했습니다.".formatted(remaining));
                            }
                            return new RefundItem(orderItem, item.quantity());
                        })
                        .toList();
            }
        };
    }

    private int remainingQuantity(OrderItem orderItem, Map<Long, Integer> alreadyRefundedQuantityByOrderItemId) {
        int alreadyRefunded = alreadyRefundedQuantityByOrderItemId.getOrDefault(orderItem.getId(), 0);
        return orderItem.getQuantity() - alreadyRefunded;
    }
}
