package com.example.commerce.domain.event.polling;

import com.example.commerce.domain.event.entity.Event;
import com.example.commerce.domain.event.entity.EventStatus;
import com.example.commerce.domain.event.repository.EventRepository;
import com.example.commerce.domain.product.entity.Product;
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

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireEvents() {
        LocalDateTime now = LocalDateTime.now();

        List<Event> expiredEvents = eventRepository.findAllByStatusAndEndAtBefore(EventStatus.ACTIVE, now);

        for (Event event : expiredEvents) {
            event.end();

            Product product = event.getProduct();
            if (product.getStock() > 0) {
                product.sale();
            } else {
                product.soldout();
            }

            log.info("[Event 종료] eventId={}, productId={}", event.getId(), product.getId());
        }
    }
}