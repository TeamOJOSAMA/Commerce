package com.example.commerce.domain.order.facade;

import com.example.commerce.domain.cart.entity.Cart;
import com.example.commerce.domain.cart.entity.CartItem;
import com.example.commerce.domain.order.dto.CreateOrderRequest;
import com.example.commerce.domain.order.dto.CreateOrderResponse;
import com.example.commerce.domain.order.entity.Order;
import com.example.commerce.domain.order.entity.OrderCancelReason;
import com.example.commerce.domain.order.entity.OrderStatus;
import com.example.commerce.domain.payment.entity.Payment;
import com.example.commerce.domain.payment.entity.PaymentStatus;
import com.example.commerce.domain.product.entity.Product;
import com.example.commerce.domain.product.entity.ProductCategory;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 주문 취소가 경위를 함께 남기는지 검증한다.
 * 주문 상태는 CANCELLED 하나뿐이라 사용자 취소·결제 실패·환불 완료는 cancelReason으로만 구분된다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderCancelIntegrationTest {

    private static final int INITIAL_STOCK = 10;
    private static final long UNIT_PRICE = 50_000L;
    private static final int ORDER_QUANTITY = 2;

    @Autowired private OrderFacade orderFacade;
    @Autowired private EntityManager entityManager;

    private Long userId;
    private Long cartItemId;

    @BeforeEach
    void setUp() {
        User user = User.of("주문자", "order-cancel@test.com", "password", UserRole.USER);
        entityManager.persist(user);
        userId = user.getId();

        Product product = new Product(1L, "기계식 키보드", "설명",
                ProductCategory.ELECTRONICS, UNIT_PRICE, INITIAL_STOCK);
        entityManager.persist(product);

        Cart cart = new Cart(user);
        entityManager.persist(cart);
        CartItem cartItem = new CartItem(cart, product, ORDER_QUANTITY);
        entityManager.persist(cartItem);
        cartItemId = cartItem.getId();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("사용자가 취소한 주문은 취소 사유가 USER_REQUEST로 남는다")
    void cancelOrder_recordsUserRequestReason() {
        CreateOrderResponse created = createOrder("cancel-key-1");

        orderFacade.cancelOrder(userId, created.orderId(), OrderCancelReason.USER_REQUEST);
        flushAndClear();

        Order order = reloadOrder(created.orderId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCanceledAt()).isNotNull();
        // 사유가 없으면 취소된 주문이 사용자 취소인지 결제 실패인지 주문만 보고는 알 수 없다.
        assertThat(order.getCancelReason()).isEqualTo(OrderCancelReason.USER_REQUEST);
    }

    @Test
    @DisplayName("사용자 취소는 READY 결제를 취소 사유 문구와 함께 FAILED로 바꾼다")
    void cancelOrder_failsReadyPaymentWithReasonMessage() {
        CreateOrderResponse created = createOrder("cancel-key-2");

        orderFacade.cancelOrder(userId, created.orderId(), OrderCancelReason.USER_REQUEST);
        flushAndClear();

        Payment payment = entityManager.find(Payment.class, created.paymentId());
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        // 결제 쪽에 남는 문구도 사유 enum 하나에서 나온다. 하드코딩 문자열로 되돌아가지 않도록 고정한다.
        assertThat(payment.getFailReason()).isEqualTo(OrderCancelReason.USER_REQUEST.getMessage());
    }

    @Test
    @DisplayName("이미 취소된 주문을 다시 취소해도 최초 취소 사유를 덮어쓰지 않는다")
    void cancelOrder_keepsFirstCancelReason() {
        CreateOrderResponse created = createOrder("cancel-key-3");
        orderFacade.cancelOrder(userId, created.orderId(), OrderCancelReason.USER_REQUEST);
        flushAndClear();

        // 다른 사유로 다시 취소를 시도한다.
        orderFacade.cancelOrder(userId, created.orderId(), OrderCancelReason.PAYMENT_FAILED);
        flushAndClear();

        assertThat(reloadOrder(created.orderId()).getCancelReason())
                .isEqualTo(OrderCancelReason.USER_REQUEST);
    }

    private CreateOrderResponse createOrder(String idempotencyKey) {
        CreateOrderResponse response = orderFacade.createOrder(userId,
                new CreateOrderRequest(List.of(cartItemId), null, idempotencyKey, null));
        flushAndClear();
        return response;
    }

    private Order reloadOrder(Long orderId) {
        return entityManager.find(Order.class, orderId);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
