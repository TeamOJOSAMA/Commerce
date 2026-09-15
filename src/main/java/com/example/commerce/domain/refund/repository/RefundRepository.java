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

    // 한 결제에 여러 건의 부분 환불이 쌓일 수 있어 발급 순서대로 모두 돌려준다.
    // 환불 가능 수량 검증(RefundService)은 이 전체 목록을 근거로 한다.
    List<Refund> findAllByPaymentId(Long paymentId);

    // 주문 상세 조립용이다. 여러 건 중 최신 환불 한 건만 대표로 보여준다.
    Optional<Refund> findFirstByPaymentIdOrderByIdDesc(Long paymentId);

    // 주문 목록 조립용이다. 결제별 최신 환불만 남기고, 환불이 없는 결제는 결과에 담기지 않는다.
    List<Refund> findAllByPaymentIdInOrderByIdDesc(List<Long> paymentIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Refund r where r.id = :refundId")
    Optional<Refund> findByIdForUpdate(@Param("refundId") Long refundId);
}
