package com.example.commerce.domain.refund.entity;

import com.example.commerce.common.entity.BaseEntity;
import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.order.entity.OrderItem;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.prefs.BackingStoreException;

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
        this.refundAmount = orderItem.getUnitPrice() * quantity;
    }

    void setRefund(Refund refund) {
        this.refund = refund;
    }
}
