package com.example.commerce.domain.event.repository;

import com.example.commerce.domain.event.entity.Event;
import com.example.commerce.domain.event.entity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    // 시작 시각은 포함하고 종료 시각은 제외한다. 여러 상품을 동일한 now로 한 번에 조회한다.
    // 상품당 진행 중 이벤트는 하나만 존재한다는 것이 이벤트 도메인의 전제이며, 주문은 이를 다시 검사하지 않는다.
    @Query("""
            select event from Event event
            where event.productId in :productIds
              and event.status = :status
              and event.startAt <= :now
              and event.endAt > :now
            """)
    List<Event> findApplicableEvents(
            @Param("productIds") List<Long> productIds,
            @Param("status") EventStatus status,
            @Param("now") LocalDateTime now
    );
}
