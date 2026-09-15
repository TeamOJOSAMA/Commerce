package com.example.commerce.domain.refund.repository;

import com.example.commerce.domain.refund.entity.Refund;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    boolean existsByPaymentId(Long paymentId);

    // 주문 상세 조립용이다. 결제당 환불 한 건(uk_refund_payment_id)이라 단건으로 반환한다.
    Optional<Refund> findByPaymentId(Long paymentId);

    // 주문 목록 조립용이다. 환불이 없는 결제는 결과에 담기지 않는다.
    List<Refund> findAllByPaymentIdIn(List<Long> paymentIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Refund r where r.id = :refundId")
    Optional<Refund> findByIdForUpdate(@Param("refundId") Long refundId);
}
