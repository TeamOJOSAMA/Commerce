package com.example.commerce.domain.order.entity;

import com.example.commerce.domain.order.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

// 배분액 컬럼에 updatable = false를 붙이면 통과하지 못하는 테스트다.
// 주문 생성은 저장(IDENTITY INSERT) 뒤에 쿠폰을 적용하므로 배분액은 UPDATE로만 DB에 닿는다.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderCouponDiscountPersistenceTest {

    @Autowired private OrderRepository orderRepository;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("저장 뒤에 적용한 쿠폰 배분액이 UPDATE로 반영되어 재조회에서도 유지된다")
    void couponDiscountShare_survivesReload() {
        // 파사드와 같은 순서다. 저장으로 주문·항목이 INSERT된 뒤에 쿠폰을 적용한다.
        Order order = orderRepository.save(new Order(1L, List.of(
                new OrderItem(100L, null, "일반 상품 A", 300L, 1),
                new OrderItem(101L, null, "일반 상품 B", 700L, 1),
                new OrderItem(200L, 10L, "이벤트 상품", 2_000L, 1)), "coupon-share-persist-key"));
        order.applyCoupon(5L, 11L);
        Map<Long, Long> expectedShares = order.getOrderItems().stream()
                .collect(Collectors.toMap(OrderItem::getId, OrderItem::getCouponDiscountShare));
        assertThat(expectedShares.values()).containsExactlyInAnyOrder(3L, 8L, 0L);

        entityManager.flush();
        entityManager.clear();

        Order reloaded = orderRepository.findById(order.getId()).orElseThrow();
        Map<Long, OrderItem> reloadedItems = reloaded.getOrderItems().stream()
                .collect(Collectors.toMap(OrderItem::getId, Function.identity()));

        assertThat(reloadedItems).hasSize(3);
        expectedShares.forEach((itemId, share) ->
                assertThat(reloadedItems.get(itemId).getCouponDiscountShare()).isEqualTo(share));
        long shareSum = reloaded.getOrderItems().stream().mapToLong(OrderItem::getCouponDiscountShare).sum();
        assertThat(shareSum).isEqualTo(reloaded.getCouponDiscountAmount());
        long paidSum = reloaded.getOrderItems().stream().mapToLong(OrderItem::getPaidAmount).sum();
        assertThat(paidSum).isEqualTo(reloaded.getPaymentAmount());
    }
}
