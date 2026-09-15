package com.example.commerce.domain.refund.entity;

import com.example.commerce.common.entity.BaseEntity;
import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.payment.entity.Payment;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "refunds", uniqueConstraints = @UniqueConstraint(
        name = "uk_refund_payment_id", columnNames = "payment_id")) // 한 결제당 환불 1건 DB 강제
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund  extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, updatable = false)
    private Payment payment;

    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RefundItem> refundItems = new ArrayList<>();

    @Column(name = "total_refund_amount", nullable = false)
    private Long totalRefundAmount;

    @Column(length = 100)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RefundStatus status;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundType refundType;

    public Refund(Payment payment, List<RefundItem> refundItems, String reason, RefundType refundType) {
        if (!payment.isPaid()) {
            throw new BusinessException(ErrorCode.REFUND_NOT_ALLOWED);
        }
        if (refundItems == null || refundItems.isEmpty()) {
            throw new BusinessException(ErrorCode.REFUND_ITEM_NOT_FOUND);
        }

        this.payment = payment;
        this.reason = reason;
        this.refundType = refundType;
        this.status = RefundStatus.REQUESTED;
        this.totalRefundAmount = refundItems.stream()
                .mapToLong(RefundItem::getRefundAmount)
                .sum();

        refundItems.forEach(this::addRefundItem);
    }

    private void addRefundItem(RefundItem refundItem) {
        refundItem.setRefund(this);
        this.refundItems.add(refundItem);
    }

    public void complete() {
        if (!status.canTransitTo(RefundStatus.COMPLETED)) {
            throw new BusinessException(ErrorCode.INVALID_REFUND_STATUS);
        }
        this.status = RefundStatus.COMPLETED;
        this.refundedAt = LocalDateTime.now();
    }
}