package com.example.commerce.domain.order.facade;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.cart.dto.AddToCartRequest;
import com.example.commerce.domain.cart.facade.CartFacade;
import com.example.commerce.domain.event.entity.Event;
import com.example.commerce.domain.event.entity.EventStatus;
import com.example.commerce.domain.event.polling.EventExpireScheduler;
import com.example.commerce.domain.order.dto.CreateOrderRequest;
import com.example.commerce.domain.order.dto.CreateOrderResponse;
import com.example.commerce.domain.order.dto.OrderPreviewResponse;
import com.example.commerce.domain.order.entity.Order;
import com.example.commerce.domain.order.repository.OrderRepository;
import com.example.commerce.domain.payment.entity.Payment;
import com.example.commerce.domain.payment.service.PaymentService;
import com.example.commerce.domain.product.entity.Product;
import com.example.commerce.domain.product.entity.ProductCategory;
import com.example.commerce.domain.product.entity.ProductStatus;
import com.example.commerce.domain.user.entity.User;
import com.example.commerce.domain.user.entity.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 이벤트 상품을 장바구니에 담은 뒤 이벤트가 끝나고 주문하는 흐름을 확인한다.
 * 장바구니는 가격을 저장하지 않으므로 단가는 주문 시점의 상품 상태와 진행 중 이벤트가 정한다.
 * H2 통과는 MySQL 잠금 동작을 보장하지 않으며, 이 테스트는 금액·상태 규칙만 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderAfterEventEndedIntegrationTest {

    private static final long REGULAR_PRICE = 10_000L;
    private static final long EVENT_PRICE = 7_000L;
    private static final int DISCOUNT_RATE = 30;
    private static final int STOCK = 10;
    private static final int ORDER_QUANTITY = 2;

    @Autowired private CartFacade cartFacade;
    @Autowired private OrderFacade orderFacade;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PaymentService paymentService;
    @Autowired private EventExpireScheduler eventExpireScheduler;
    @Autowired private EntityManager entityManager;

    private Long userId;
    private Long productId;
    private Long eventId;
    private Long cartItemId;

    @BeforeEach
    void setUp() {
        User user = User.of("주문자", "event-order@test.com", "password", UserRole.USER);
        entityManager.persist(user);
        userId = user.getId();

        Product product = new Product(1L, "타임세일 상품", "설명",
                ProductCategory.ELECTRONICS, REGULAR_PRICE, STOCK);
        entityManager.persist(product);
        productId = product.getId();

        // 판매자가 이벤트를 시작한 상태다. 1시간 뒤에 끝나므로 장바구니에 담는 시점에는 진행 중이다.
        Event event = new Event(product, DISCOUNT_RATE, EVENT_PRICE, STOCK,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        entityManager.persist(event);
        product.event();
        eventId = event.getId();
        entityManager.flush();
        entityManager.clear();

        // 이벤트 진행 중에 실제 담기 경로로 장바구니에 넣는다.
        cartItemId = cartFacade.addCartItem(userId, productId, new AddToCartRequest(ORDER_QUANTITY))
                .cartItemId();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("이벤트 진행 중에 주문하면 이벤트 가격으로 결제 금액이 계산된다")
    void createOrder_duringEvent_usesEventPrice() {
        OrderPreviewResponse preview = orderFacade.getOrderPreview(userId, List.of(cartItemId), null);
        assertThat(preview.items()).singleElement().satisfies(item -> {
            assertThat(item.available()).isTrue();
            assertThat(item.eventId()).isEqualTo(eventId);
            assertThat(item.unitPrice()).isEqualTo(EVENT_PRICE);
        });

        CreateOrderResponse response = orderFacade.createOrder(userId,
                new CreateOrderRequest(List.of(cartItemId), null, "event-key-during", preview.paymentAmount()));

        assertThat(response.paymentAmount()).isEqualTo(EVENT_PRICE * ORDER_QUANTITY);
        Order order = orderRepository.findById(response.orderId()).orElseThrow();
        assertThat(order.getOrderItems()).singleElement().satisfies(item -> {
            assertThat(item.getEventId()).isEqualTo(eventId);
            assertThat(item.getUnitPrice()).isEqualTo(EVENT_PRICE);
        });
    }

    @Test
    @DisplayName("이벤트가 끝나고 스케줄러가 상품을 되돌린 뒤 주문하면 정가로 결제 금액이 계산된다")
    void createOrder_afterEventEndedAndSchedulerRan_usesRegularPrice() {
        endEvent();
        eventExpireScheduler.expireEvents();
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(Product.class, productId).getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(entityManager.find(Event.class, eventId).getStatus()).isEqualTo(EventStatus.ENDED);

        // 주문서에는 구매 불가가 아니라 정가 항목으로 올라간다.
        OrderPreviewResponse preview = orderFacade.getOrderPreview(userId, List.of(cartItemId), null);
        assertThat(preview.hasUnavailableItem()).isFalse();
        assertThat(preview.items()).singleElement().satisfies(item -> {
            assertThat(item.eventId()).isNull();
            assertThat(item.unitPrice()).isEqualTo(REGULAR_PRICE);
        });
        assertThat(preview.paymentAmount()).isEqualTo(REGULAR_PRICE * ORDER_QUANTITY);

        CreateOrderResponse response = orderFacade.createOrder(userId,
                new CreateOrderRequest(List.of(cartItemId), null, "event-key-after", preview.paymentAmount()));

        assertThat(response.totalAmount()).isEqualTo(REGULAR_PRICE * ORDER_QUANTITY);
        assertThat(response.paymentAmount()).isEqualTo(REGULAR_PRICE * ORDER_QUANTITY);
        Order order = orderRepository.findById(response.orderId()).orElseThrow();
        assertThat(order.getOrderItems()).singleElement().satisfies(item -> {
            assertThat(item.getEventId()).isNull();
            assertThat(item.getUnitPrice()).isEqualTo(REGULAR_PRICE);
        });
        Payment payment = paymentService.findPaymentByOrderId(response.orderId()).orElseThrow();
        assertThat(payment.getAmount()).isEqualTo(REGULAR_PRICE * ORDER_QUANTITY);
    }

    @Test
    @DisplayName("이벤트 중에 확인한 금액으로 종료 후 주문하면 금액 불일치로 거부한다")
    void createOrder_withAmountConfirmedDuringEvent_rejectsAfterEventEnded() {
        // 사용자가 이벤트 중에 주문서를 열어 이벤트 가격을 확인했다.
        long confirmedDuringEvent = orderFacade.getOrderPreview(userId, List.of(cartItemId), null).paymentAmount();
        assertThat(confirmedDuringEvent).isEqualTo(EVENT_PRICE * ORDER_QUANTITY);

        endEvent();
        eventExpireScheduler.expireEvents();
        entityManager.flush();
        entityManager.clear();

        // 실제 금액은 정가로 바뀌었으므로 결제를 만들지 않고 다시 확인하게 한다.
        assertThatThrownBy(() -> orderFacade.createOrder(userId,
                new CreateOrderRequest(List.of(cartItemId), null, "event-key-mismatch", confirmedDuringEvent)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_PAYMENT_AMOUNT_MISMATCH);
    }

    @Test
    @DisplayName("이벤트가 끝났지만 스케줄러가 돌기 전이어도 정가로 주문된다")
    void createOrder_afterEventEndedBeforeScheduler_usesRegularPrice() {
        endEvent();
        // 스케줄러는 아직 돌지 않아 상품 상태는 ON_EVENT 그대로다.
        assertThat(entityManager.find(Product.class, productId).getStatus()).isEqualTo(ProductStatus.ON_EVENT);

        OrderPreviewResponse preview = orderFacade.getOrderPreview(userId, List.of(cartItemId), null);
        assertThat(preview.hasUnavailableItem()).isFalse();
        assertThat(preview.items()).singleElement().satisfies(item -> {
            assertThat(item.eventId()).isNull();
            assertThat(item.unitPrice()).isEqualTo(REGULAR_PRICE);
        });

        CreateOrderResponse response = orderFacade.createOrder(userId,
                new CreateOrderRequest(List.of(cartItemId), null, "event-key-window", preview.paymentAmount()));
        assertThat(response.paymentAmount()).isEqualTo(REGULAR_PRICE * ORDER_QUANTITY);
    }

    /**
     * 시간이 흘러 이벤트가 끝난 것을 흉내 낸다.
     * Event에는 종료 시각을 바꾸는 메서드가 없고 테스트에서 실제로 기다릴 수 없으므로,
     * 주문·스케줄러 조회 조건이 보는 end_at 컬럼만 과거로 옮긴다.
     */
    private void endEvent() {
        entityManager.createQuery("update Event e set e.endAt = :endAt where e.id = :eventId")
                .setParameter("endAt", LocalDateTime.now().minusMinutes(1))
                .setParameter("eventId", eventId)
                .executeUpdate();
        entityManager.clear();
    }
}
