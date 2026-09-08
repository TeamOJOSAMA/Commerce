package com.example.commerce.domain.payment.entity;

import com.example.commerce.common.entity.BaseEntity;
import com.example.commerce.common.error.BusinessException;
import com.example.commerce.common.error.ErrorCode;
import com.example.commerce.domain.refund.entity.RefundStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.core.annotation.Order;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String failReason;

    private LocalDateTime paidAt;

    private Payment(Order order, Long amount) {
        this.order = order;
        this.amount = amount;
        this.status = PaymentStatus.READY;
    }

    public static Payment of(Order order, Long amount) {
        return new Payment(order, amount);
    }

    public void fail(String failReason){
        vaildateReady();
        this.status = PaymentStatus.FAILED;
        this.failReason = failReason;
    }

    // PAID/FAILED로 이미 종결된 결제는 다시 전이될 수 없어야 하므로
    private void vaildateReady() {
        if (this.status != PaymentStatus.READY) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
    }

    public boolean isPaid() {
        return this.status == PaymentStatus.PAID;
    }
}
