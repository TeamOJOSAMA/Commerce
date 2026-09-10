package com.example.commerce.domain.payment.repository;

import com.example.commerce.domain.payment.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);
    boolean existsByOrderId(Long orderId);

    // 결제 엔티티를 먼저 로딩하지 않고 주문 잠금에 필요한 ID만 가져온다.
    @Query("select payment.order.id from Payment payment where payment.id = :paymentId")
    Optional<Long> findOrderIdByPaymentId(@Param("paymentId") Long paymentId);

    // 승인·실패 처리에서 주문 잠금을 획득한 다음 결제 상태를 잠금 조회한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.id = :paymentId")
    Optional<Payment> findByIdForUpdate(@Param("paymentId") Long paymentId);

    // 주문 취소에서 사용한다. 결제가 없는 주문을 위해 Optional로 반환한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.order.id = :orderId")
    Optional<Payment> findByOrderIdForUpdate(@Param("orderId") Long orderId);
}
