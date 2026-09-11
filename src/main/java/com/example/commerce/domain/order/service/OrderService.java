package com.example.commerce.domain.order.service;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.order.dto.OrderSummaryResponse;
import com.example.commerce.domain.order.entity.Order;
import com.example.commerce.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 주문 저장·조회에 집중하고, 재고·쿠폰·결제와의 처리 순서는 OrderFacade에서 조정한다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;

    // Facade의 트랜잭션에 참여해 주문과 cascade로 연결된 항목을 함께 저장한다.
    @Transactional
    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }

    // 사용자의 주문을 생성 시각 내림차순으로 가져와 목록용 DTO로 변환한다.
    public List<OrderSummaryResponse> getOrders(Long userId) {
        return orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderSummaryResponse::from)
                .toList();
    }

    // 없는 주문과 다른 사람의 주문을 같은 오류로 처리해 타인의 주문 존재 여부를 노출하지 않는다.
    public Order getOwnedOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    // 취소 트랜잭션에서 소유 주문을 잠그며 호출자의 트랜잭션이 끝날 때까지 잠금을 유지한다.
    @Transactional
    public Order getOwnedOrderForUpdate(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }
}
