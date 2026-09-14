package com.example.commerce.domain.event.polling;

import com.example.commerce.domain.event.entity.Event;
import com.example.commerce.domain.event.entity.EventStatus;
import com.example.commerce.domain.event.repository.EventRepository;
import com.example.commerce.domain.product.entity.Product;
import com.example.commerce.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventExpireScheduler {

    private final EventRepository eventRepository;
    private final ProductRepository productRepository;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireEvents() {
        LocalDateTime now = LocalDateTime.now();

        List<Event> expiredEvents = eventRepository.findAllByStatusAndEndAtBefore(EventStatus.ACTIVE, now);

        if (expiredEvents.isEmpty()) {
            return;
        }

        List<Long> productIds = expiredEvents.stream()
                .map(event -> event.getProduct().getId())
                .distinct()
                .toList();

        // 재고 확인과 상태 변경 사이에 다른 주문이 끼어들지 못하게 잠금 조회
        List<Product> lockedProducts = productRepository.findAllByIdsForUpdate(productIds);

        for (Event event : expiredEvents) {
            event.end();

            Product product = lockedProducts.stream()
                    .filter(p -> p.getId().equals(event.getProduct().getId()))
                    .findFirst()
                    .orElseThrow();

            if (product.getStock() > 0) {
                product.sale();
            } else {
                product.soldout();
            }

            log.info("[Event 종료] eventId={}, productId={}", event.getId(), product.getId());
        }
    }
}