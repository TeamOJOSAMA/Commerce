package com.example.commerce.domain.order.entity;

import com.example.commerce.common.entity.BaseEntity;
import com.example.commerce.common.error.BusinessException;
import com.example.commerce.common.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;

    @Column(name = "event_id", updatable = false)
    private Long eventId;

    @Column(name = "product_name", nullable = false, updatable = false)
    private String productName;

    @Column(name = "unit_price", nullable = false, updatable = false)
    private Long unitPrice;

    @Column(nullable = false, updatable = false)
    private Integer quantity;

    public OrderItem(
            Long productId,
            Long eventId,
            String productName,
            Long unitPrice,
            Integer quantity
    ) {
        if (unitPrice == null || unitPrice < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "상품 단가는 0 이상이어야 합니다.");
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "상품 수량은 1 이상이어야 합니다.");
        }

        this.productId = productId;
        this.eventId = eventId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    void setOrder(Order order) {
        if (order == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "주문은 필수입니다.");
        }

        if (this.order != null && this.order != order) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 다른 주문에 등록된 항목입니다.");
        }

        this.order = order;
    }

    public boolean hasAppliedEvent() {
        return eventId != null;
    }

    public Long getSubTotal() {
        return Math.multiplyExact(unitPrice, quantity);
    }
}
