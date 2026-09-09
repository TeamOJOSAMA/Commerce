package com.example.commerce.domain.payment.entity;

import com.example.commerce.common.entity.BaseEntity;
import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.order.entity.Order;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "payments", uniqueConstraints = @UniqueConstraint(
        name = "uk_payments_order_id", columnNames = "order_id"))   // order_id 유니크 제약: "한 주문당 결제 1건"을 DB가 강제
                                                                    // existsByOrderId 체크와 save 사이 레이스로 동시 요청이 둘 다 통과해도
                                                                    // 두 번째 INSERT를 DB가 막아 중복 결제 생성이 원천 차단된다.
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
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(length = 50)
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

    // 상태전이: 결제 승인 - READY 상태에서만 승인 가능 (중복 승인 자동 차단)
    public void approve() {
        validateReadyStatus();
        this.status = PaymentStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    // 상태전이: 결제 실패 - READY 상태에서만 실패 처리 가능
    public void fail(String failReason) {
        validateReadyStatus();
        this.status = PaymentStatus.FAILED;
        this.failReason = failReason;
    }

    // 상태전이: 결제 취소 - 환불 시작 시 결제 완료 건에 한해 호출
    public void cancel() {
        validatePaidStatus();
        this.status = PaymentStatus.CANCELED;
    }

    public boolean isPaid() {
        return this.status == PaymentStatus.PAID;
    }

    private void validateReadyStatus() {
        if (this.status != PaymentStatus.READY) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
    }

    private void validatePaidStatus() {
        if (this.status != PaymentStatus.PAID) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
    }
}