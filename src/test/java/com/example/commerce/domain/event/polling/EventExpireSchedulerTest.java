package com.example.commerce.domain.event.polling;

import com.example.commerce.domain.event.entity.Event;
import com.example.commerce.domain.event.entity.EventStatus;
import com.example.commerce.domain.product.entity.Product;
import com.example.commerce.domain.product.entity.ProductCategory;
import com.example.commerce.domain.product.entity.ProductStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 이벤트 종료 스케줄러가 만료 이벤트를 ENDED로 바꾸고 상품 상태를 되돌리는지 확인한다.
 * 한 행의 상태 전이 실패가 회차 전체를 롤백시키지 않는 것이 핵심이다.
 * H2 통과는 MySQL 잠금 동작을 보장하지 않으며, 이 테스트는 상태 규칙만 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventExpireSchedulerTest {

    private static final long REGULAR_PRICE = 10_000L;
    private static final long EVENT_PRICE = 7_000L;
    private static final int DISCOUNT_RATE = 30;

    @Autowired private EventExpireScheduler eventExpireScheduler;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("만료 이벤트 상품 중 하나가 이미 다른 상태여도 회차가 실패하지 않고 나머지 상품은 되돌린다")
    void expireEvents_skipsProductWhoseStatusSellerAlreadyChanged() {
        Product onEvent = persistProduct("이벤트 유지 상품", 10);
        Product changedBySeller = persistProduct("판매자 변경 상품", 10);
        Event onEventExpired = persistExpiredEvent(onEvent);
        Event changedExpired = persistExpiredEvent(changedBySeller);
        onEvent.event();
        changedBySeller.event();
        // 판매자가 이벤트 도중 상품을 품절로 바꿨다. 이벤트 행은 ACTIVE로 남는다.
        changedBySeller.soldout();
        flushAndClear();

        // 이전 구현은 여기서 INVALID_PRODUCT_STATUS를 던져 두 이벤트 모두 종료되지 못했다.
        assertThatCode(() -> eventExpireScheduler.expireEvents()).doesNotThrowAnyException();
        flushAndClear();

        assertThat(find(Event.class, onEventExpired.getId()).getStatus()).isEqualTo(EventStatus.ENDED);
        assertThat(find(Event.class, changedExpired.getId()).getStatus()).isEqualTo(EventStatus.ENDED);
        assertThat(find(Product.class, onEvent.getId()).getStatus()).isEqualTo(ProductStatus.ON_SALE);
        // 판매자가 정한 상태는 덮어쓰지 않는다.
        assertThat(find(Product.class, changedBySeller.getId()).getStatus()).isEqualTo(ProductStatus.SOLDOUT);
    }

    @Test
    @DisplayName("만료 이벤트 상품은 재고가 있으면 ON_SALE, 없으면 SOLDOUT으로 되돌린다")
    void expireEvents_restoresStatusByStock() {
        Product inStock = persistProduct("재고 있는 상품", 10);
        Product outOfStock = persistProduct("재고 없는 상품", 3);
        persistExpiredEvent(inStock);
        persistExpiredEvent(outOfStock);
        inStock.event();
        outOfStock.event();
        // 이벤트 중에 전량 판매되었다. decreaseStock()은 상태를 바꾸지 않는다.
        outOfStock.decreaseStock(3);
        flushAndClear();

        eventExpireScheduler.expireEvents();
        flushAndClear();

        assertThat(find(Product.class, inStock.getId()).getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(find(Product.class, outOfStock.getId()).getStatus()).isEqualTo(ProductStatus.SOLDOUT);
    }

    @Test
    @DisplayName("아직 끝나지 않은 이벤트와 상품은 건드리지 않는다")
    void expireEvents_leavesRunningEventUntouched() {
        Product running = persistProduct("진행 중 상품", 10);
        Event runningEvent = persistEvent(running, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        running.event();
        flushAndClear();

        eventExpireScheduler.expireEvents();
        flushAndClear();

        assertThat(find(Event.class, runningEvent.getId()).getStatus()).isEqualTo(EventStatus.ACTIVE);
        assertThat(find(Product.class, running.getId()).getStatus()).isEqualTo(ProductStatus.ON_EVENT);
    }

    @Test
    @DisplayName("같은 상품에 만료 이벤트가 두 건이어도 예외 없이 모두 종료하고 상품은 한 번만 되돌린다")
    void expireEvents_handlesTwoExpiredEventsOnSameProduct() {
        // 상품당 진행 중 이벤트는 하나라는 것이 이벤트 도메인의 전제지만, 데이터가 어긋나도 회차가 막히면 안 된다.
        Product product = persistProduct("중복 이벤트 상품", 10);
        Event first = persistExpiredEvent(product);
        Event second = persistExpiredEvent(product);
        product.event();
        flushAndClear();

        assertThatCode(() -> eventExpireScheduler.expireEvents()).doesNotThrowAnyException();
        flushAndClear();

        assertThat(find(Event.class, first.getId()).getStatus()).isEqualTo(EventStatus.ENDED);
        assertThat(find(Event.class, second.getId()).getStatus()).isEqualTo(EventStatus.ENDED);
        assertThat(find(Product.class, product.getId()).getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    private Product persistProduct(String name, int stock) {
        Product product = new Product(1L, name, "설명", ProductCategory.ELECTRONICS, REGULAR_PRICE, stock);
        entityManager.persist(product);
        return product;
    }

    // 2시간 전에 시작해 1분 전에 끝난 이벤트다. 스케줄러가 아직 돌지 않아 ACTIVE로 남아 있다.
    private Event persistExpiredEvent(Product product) {
        return persistEvent(product, LocalDateTime.now().minusHours(2), LocalDateTime.now().minusMinutes(1));
    }

    private Event persistEvent(Product product, LocalDateTime startAt, LocalDateTime endAt) {
        Event event = new Event(product, DISCOUNT_RATE, EVENT_PRICE, product.getStock(), startAt, endAt);
        entityManager.persist(event);
        return event;
    }

    private <T> T find(Class<T> type, Long id) {
        return entityManager.find(type, id);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
