package com.example.commerce.domain.event.polling;

import com.example.commerce.domain.event.entity.Event;
import com.example.commerce.domain.event.entity.EventStatus;
import com.example.commerce.domain.event.repository.EventRepository;
import com.example.commerce.domain.product.entity.Product;
import com.example.commerce.domain.product.entity.ProductStatus;
import com.example.commerce.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventExpireScheduler {

    private final EventRepository eventRepository;
    private final ProductRepository productRepository;

    /**
     * 종료 시각이 지난 ACTIVE 이벤트를 ENDED로 바꾸고 상품을 이벤트 전 상태로 되돌린다.
     *
     * <p>한 트랜잭션이므로 이벤트 하나에서 예외가 나면 회차 전체가 롤백되고, 다음 회차도 같은 행에서 다시 실패한다.
     * 실제로 판매자가 이벤트 도중 상품 상태를 바꾸면(ON_EVENT → SOLDOUT·ON_SALE) 그 이벤트가 만료될 때
     * {@code sale()}·{@code soldout()}이 INVALID_PRODUCT_STATUS를 던져 이후 모든 이벤트 종료가 매분 막혔다.
     * 그래서 상품 상태는 ON_EVENT일 때만 바꾸고, 그 외의 상품은 이벤트만 종료하고 넘어간다(2026-09-15).</p>
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireEvents() {
        LocalDateTime now = LocalDateTime.now();

        List<Event> expiredEvents = eventRepository.findAllByStatusAndEndAtBefore(EventStatus.ACTIVE, now);
        if (expiredEvents.isEmpty()) {
            return;
        }

        // 재고 확인과 상태 변경 사이에 다른 주문이 끼어들지 못하게 잠금 조회한다.
        // 주문·취소와 같은 규칙으로 상품 ID를 중복 제거·오름차순 정렬한 뒤 한 번에 잠근다.
        List<Long> productIds = expiredEvents.stream()
                .map(event -> event.getProduct().getId())
                .distinct()
                .sorted()
                .toList();
        Map<Long, Product> lockedProducts = productRepository.findAllByIdsForUpdate(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        for (Event event : expiredEvents) {
            // 이벤트 종료는 상품 상태와 무관하게 항상 반영한다. 여기서 끝내지 않으면 매 회차 다시 조회된다.
            event.end();

            Long productId = event.getProduct().getId();
            Product product = lockedProducts.get(productId);
            if (product == null) {
                log.warn("[Event 종료] 상품 없음 - eventId={}, productId={}", event.getId(), productId);
                continue;
            }

            // 판매자가 이벤트 도중 상태를 바꿨거나, 같은 상품의 다른 이벤트가 이 회차에서 먼저 처리된 경우다.
            // 판매자가 정한 상태를 덮어쓰지 않고 이벤트만 종료한다.
            if (product.getStatus() != ProductStatus.ON_EVENT) {
                log.info("[Event 종료] 상품 상태 유지 - eventId={}, productId={}, status={}",
                        event.getId(), productId, product.getStatus());
                continue;
            }

            if (product.getStock() > 0) {
                product.sale();
            } else {
                product.soldout();
            }

            log.info("[Event 종료] eventId={}, productId={}, status={}",
                    event.getId(), productId, product.getStatus());
        }
    }
}
