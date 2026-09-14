package com.example.commerce.domain.order.facade;

import com.example.commerce.domain.order.dto.OrderDisplayStatus;
import com.example.commerce.domain.order.dto.OrderSummaryResponse;
import com.example.commerce.domain.order.entity.Order;
import com.example.commerce.domain.order.entity.OrderItem;
import com.example.commerce.domain.payment.entity.Payment;
import com.example.commerce.domain.product.entity.Product;
import com.example.commerce.domain.product.entity.ProductCategory;
import com.example.commerce.domain.user.entity.User;
import com.example.commerce.domain.user.entity.UserRole;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 주문 목록은 진행 상태와 대표 상품명을 함께 반환하며 페이징은 최신순으로 고정한다.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderListIntegrationTest {

    @Autowired private OrderFacade orderFacade;
    @Autowired private EntityManager entityManager;

    private Long userId;

    @BeforeEach
    void setUp() {
        User user = User.of("주문자", "order-list@test.com", "password", UserRole.USER);
        entityManager.persist(user);
        userId = user.getId();

        Product keyboard = new Product(1L, "기계식 키보드", "설명", ProductCategory.ELECTRONICS, 50_000L, 100);
        Product mouse = new Product(1L, "무선 마우스", "설명", ProductCategory.ELECTRONICS, 20_000L, 100);
        entityManager.persist(keyboard);
        entityManager.persist(mouse);

        // 첫 번째 주문: 항목 두 종류, 결제 승인 완료
        Order paidOrder = new Order(userId, List.of(
                new OrderItem(keyboard.getId(), null, keyboard.getName(), 50_000L, 1),
                new OrderItem(mouse.getId(), null, mouse.getName(), 20_000L, 2)
        ), "key-paid");
        entityManager.persist(paidOrder);
        Payment paidPayment = Payment.of(paidOrder, paidOrder.getPaymentAmount());
        paidPayment.approve();
        // 실제 승인은 결제와 주문 상태를 함께 바꾼다. 주문을 확정하지 않으면 실제로 없는 조합이 만들어진다.
        paidOrder.confirm();
        entityManager.persist(paidPayment);

        // 두 번째 주문: 항목 한 종류, 결제 대기
        Order readyOrder = new Order(userId, List.of(
                new OrderItem(mouse.getId(), null, mouse.getName(), 20_000L, 3)
        ), "key-ready");
        entityManager.persist(readyOrder);
        entityManager.persist(Payment.of(readyOrder, readyOrder.getPaymentAmount()));

        // 세 번째 주문: 결제 없이 남은 주문
        entityManager.persist(new Order(userId, List.of(
                new OrderItem(keyboard.getId(), null, keyboard.getName(), 50_000L, 1)
        ), "key-no-payment"));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("항목이 여럿이면 주문명을 \"상품명 외 N건\"으로 요약하고 진행 상태를 함께 반환한다")
    void getOrders_summarizesOrderNameAndDisplayStatus() {
        Page<OrderSummaryResponse> orders = orderFacade.getOrders(userId, PageRequest.of(0, 10));

        assertThat(orders.getTotalElements()).isEqualTo(3);

        OrderSummaryResponse paidOrder = orders.getContent().stream()
                .filter(order -> order.displayStatus() == OrderDisplayStatus.PAID)
                .findFirst()
                .orElseThrow();
        assertThat(paidOrder.orderName()).isEqualTo("기계식 키보드 외 1건");
        assertThat(paidOrder.totalQuantity()).isEqualTo(3);
        assertThat(paidOrder.paymentAmount()).isEqualTo(90_000L);
        // 결제 상태 대신 진입 키만 내려준다. 승인 시각 같은 내역은 결제 단건 조회의 몫이다.
        assertThat(paidOrder.paymentId()).isNotNull();
        assertThat(paidOrder.refundId()).isNull();
    }

    @Test
    @DisplayName("항목이 하나인 주문의 주문명은 상품명만 사용한다")
    void getOrders_singleItemOrderNameHasNoSuffix() {
        Page<OrderSummaryResponse> orders = orderFacade.getOrders(userId, PageRequest.of(0, 10));

        assertThat(orders.getContent())
                .filteredOn(order -> order.paymentId() == null)
                .singleElement()
                .satisfies(order -> assertThat(order.orderName()).isEqualTo("기계식 키보드"));
    }

    @Test
    @DisplayName("결제가 없는 주문도 목록에서 빠지지 않고 결제 ID만 null로 반환한다")
    void getOrders_keepsOrderWithoutPayment() {
        Page<OrderSummaryResponse> orders = orderFacade.getOrders(userId, PageRequest.of(0, 10));

        // 결제 없는 주문도 대기 중으로 보이며 목록에서 사라지지 않는다.
        assertThat(orders.getContent())
                .filteredOn(order -> order.paymentId() == null)
                .singleElement()
                .satisfies(order -> assertThat(order.displayStatus())
                        .isEqualTo(OrderDisplayStatus.PAYMENT_PENDING));
        assertThat(orders.getContent())
                .filteredOn(order -> order.displayStatus() == OrderDisplayStatus.PAYMENT_PENDING)
                .hasSize(2);
    }

    @Test
    @DisplayName("주문과 항목을 한 번에 읽고 결제도 일괄 조회해 주문 수에 비례해 쿼리가 늘지 않는다")
    void getOrders_doesNotGrowQueriesPerOrder() {
        Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        Page<OrderSummaryResponse> orders = orderFacade.getOrders(userId, PageRequest.of(0, 10));
        // 항목과 결제를 읽지 않은 채 쿼리 수만 세지 않도록 응답을 실제로 조회한다.
        orders.getContent().forEach(order -> {
            assertThat(order.orderName()).isNotBlank();
            assertThat(order.totalQuantity()).isPositive();
        });

        // 항목을 함께 읽는 주문 페이지 조회 + 결제 일괄 조회 + 환불 일괄 조회로 3건이다.
        // 첫 페이지에 전체가 담기면 Spring Data가 전체 건수 쿼리를 생략한다.
        // 주문마다 항목·결제·환불을 따로 읽으면 이 수가 주문 수만큼 늘어난다.
        // 결제와 환불을 한 번에 조인해 읽으면 2건으로 줄일 수 있다(docs/refund-multiple-partial-todo.md).
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("요청한 정렬 조건과 무관하게 생성 시각 내림차순으로 페이징한다")
    void getOrders_alwaysLatestFirst() {
        Page<OrderSummaryResponse> firstPage = orderFacade.getOrders(
                userId, PageRequest.of(0, 2, org.springframework.data.domain.Sort.by("paymentAmount")));

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getContent())
                .extracting(OrderSummaryResponse::createdAt)
                .isSortedAccordingTo(java.util.Comparator.reverseOrder());

        Page<OrderSummaryResponse> secondPage = orderFacade.getOrders(userId, PageRequest.of(1, 2));
        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(secondPage.getContent().get(0).createdAt())
                .isBeforeOrEqualTo(firstPage.getContent().get(1).createdAt());
    }

    @Test
    @DisplayName("페이지 크기만큼만 읽는다 - 항목 페치 조인이 메모리 페이징으로 떨어지지 않는다")
    void getOrders_appliesPagingInSqlNotInMemory() {
        // setUp의 3건에 더해 사용자의 주문을 7건으로 늘린다.
        for (int i = 0; i < 4; i++) {
            entityManager.persist(new Order(userId, List.of(
                    new OrderItem(1L, null, "추가 상품" + i, 1_000L, 1)), "key-extra-" + i));
        }
        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        Page<OrderSummaryResponse> firstPage = orderFacade.getOrders(userId, PageRequest.of(0, 2));
        firstPage.getContent().forEach(order -> assertThat(order.orderName()).isNotBlank());

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(7);
        // @EntityGraph로 컬렉션을 함께 읽는 페이지 조회는 Hibernate가 메모리 페이징으로 떨어지기 쉽다.
        // 그 경우에도 반환 건수는 2로 맞기 때문에 실제로 몇 건을 읽었는지로 확인한다.
        assertThat(statistics.getEntityStatistics(Order.class.getName()).getLoadCount()).isEqualTo(2L);
    }
}
