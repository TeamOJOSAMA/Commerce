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
    // 조회 결과가 상품당 두 건 이상이면 Facade가 중복 이벤트로 거부한다.
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
