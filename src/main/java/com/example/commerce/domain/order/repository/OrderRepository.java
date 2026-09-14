package com.example.commerce.domain.order.repository;

import com.example.commerce.domain.order.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 목록 DTO가 주문명과 총수량을 만들 때 쓰는 항목을 함께 읽는다.
    // 정렬·페이징은 루트 주문에만 적용되고 항목은 그 결과에 조인된다.
    @EntityGraph(attributePaths = "orderItems")
    Page<Order> findAllByUserId(Long userId, Pageable pageable);

    // 같은 키로 다시 들어온 주문 요청인지 확인한다. 응답에 항목이 필요하지 않아 항목은 읽지 않는다.
    Optional<Order> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

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
