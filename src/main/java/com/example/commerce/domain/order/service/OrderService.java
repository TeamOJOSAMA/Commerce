package com.example.commerce.domain.order.service;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.order.entity.Order;
import com.example.commerce.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// 주문 저장·조회에 집중하고, 재고·쿠폰·결제와의 처리 순서는 OrderFacade에서 조정한다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final OrderRepository orderRepository;

    // Facade의 트랜잭션에 참여해 주문과 cascade로 연결된 항목을 함께 저장한다.
    @Transactional
    public Order saveOrder(Order order) {
        try {
            // 사전 조회를 함께 통과한 동시 요청도 멱등성 키 유일성 제약으로 거부한다.
            // 커밋까지 미루지 않고 여기서 반영해야 아래에서 위반을 잡을 수 있다.
            return orderRepository.saveAndFlush(order);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_ORDER_REQUEST);
        }
    }

    // 같은 키로 이미 만들어진 주문이다. 있으면 재고를 다시 차감하지 않고 그대로 응답한다.
    public Optional<Order> findOrderByIdempotencyKey(Long userId, String idempotencyKey) {
        return orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
    }

    /**
     * 사용자의 주문을 생성 시각 내림차순으로 한 페이지 조회한다.
     * 잘못된 정렬 필드로 조회가 실패하지 않도록 요청의 정렬 조건은 받지 않고 최신순으로 고정한다.
     */
    public Page<Order> getOrders(Long userId, Pageable pageable) {
        return orderRepository.findAllByUserId(userId, toLatestFirst(pageable));
    }

    private Pageable toLatestFirst(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), LATEST_FIRST);
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
