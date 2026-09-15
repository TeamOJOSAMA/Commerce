package com.example.commerce.domain.refund.repository;

import com.example.commerce.domain.refund.entity.RefundItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundItemRepository extends JpaRepository<RefundItem, Long> {

    // 이미 접수된(요청/완료 불문) 환불에서 각 주문 항목이 얼마나 차지됐는지 집계하기 위한 조회다.
    List<RefundItem> findAllByOrderItem_IdIn(List<Long> orderItemIds);
}
