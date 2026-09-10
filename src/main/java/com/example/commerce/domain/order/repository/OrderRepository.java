package com.example.commerce.domain.order.repository;

import com.example.commerce.domain.order.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 목록 DTO의 총수량 계산에 필요한 항목을 함께 읽고 최근 생성된 주문부터 반환한다.
    @EntityGraph(attributePaths = "orderItems")
    List<Order> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    // 상세 조회에 필요한 항목을 가져오면서 주문 소유자도 조회 조건에 포함한다.
    @EntityGraph(attributePaths = "orderItems")
    Optional<Order> findByIdAndUserId(Long orderId, Long userId);

    // 결제 처리에서 주문을 먼저 잠그기 위한 조회다. 사용자 소유권 조건은 포함하지 않는다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :orderId")
    Optional<Order> findByIdForUpdate(@Param("orderId") Long orderId);

    // 사용자 취소 요청은 소유권을 확인하면서 잠가 승인·재취소와의 상태 변경을 직렬화한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :orderId and o.userId = :userId")
    Optional<Order> findByIdAndUserIdForUpdate(
            @Param("orderId") Long orderId, @Param("userId") Long userId
    );
}
