package com.example.commerce.domain.refund.repository;

import com.example.commerce.domain.refund.entity.RefundItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundItemRepository extends JpaRepository<RefundItem, Long> {
}
