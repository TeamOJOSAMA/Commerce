package com.example.commerce.domain.refund.repository;

import com.example.commerce.domain.refund.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    boolean existByPaymentId(Long paymentId);
}
