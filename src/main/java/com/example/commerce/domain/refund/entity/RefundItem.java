package com.example.commerce.domain.refund.entity;

import com.example.commerce.common.entity.BaseEntity;
import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.order.entity.OrderItem;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Entity
@Table(name = "refund_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_id", nullable = false, updatable = false)
    private Refund refund;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false, updatable = false)
    private OrderItem orderItem;

    @Column(nullable = false, updatable = false)
    private Integer quantity;

    @Column(name = "refund_amount", nullable = false, updatable = false)
    private Long refundAmount;

    public RefundItem(OrderItem orderItem, Integer quantity) {
        if (quantity == null || quantity < 1 || quantity > orderItem.getQuantity()) {
            throw new BusinessException(ErrorCode.REFUND_ITEM_NOT_FOUND, "환불 수량이 올바르지 않습니다.");
        }

        this.orderItem = orderItem;
        this.quantity = quantity;
        this.refundAmount = calculateRefundAmount(orderItem, quantity);
    }

    // 환불액은 단가가 아니라 항목의 실결제액(소계 - 쿠폰 배분액)을 기준으로 한다.
    // 단가 × 수량으로 계산하면 쿠폰을 쓴 주문의 전액 환불이 실제 결제액보다 커진다.
    // 전량 환불은 실결제액 그대로 돌려줘 전액 환불 합계가 payment.amount와 정확히 일치하고,
    // 수량 일부 환불은 실결제액 × 환불 수량 ÷ 주문 수량(원 미만 버림)이다.
    // 실결제액 × 수량은 long을 넘을 수 있어 BigInteger로 계산한다. 결과는 실결제액 이하라 long에 들어간다.
    private static long calculateRefundAmount(OrderItem orderItem, int quantity) {
        long paidAmount = orderItem.getPaidAmount();
        int orderedQuantity = orderItem.getQuantity();

        if (quantity == orderedQuantity) {
            return paidAmount;
        }

        return BigInteger.valueOf(paidAmount)
                .multiply(BigInteger.valueOf(quantity))
                .divide(BigInteger.valueOf(orderedQuantity))
                .longValueExact();
    }

    void setRefund(Refund refund) {
        this.refund = refund;
    }
}
