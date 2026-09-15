package com.example.commerce.domain.refund;

import com.example.commerce.domain.cart.entity.Cart;
import com.example.commerce.domain.cart.entity.CartItem;
import com.example.commerce.domain.order.dto.CreateOrderRequest;
import com.example.commerce.domain.order.dto.CreateOrderResponse;
import com.example.commerce.domain.order.dto.OrderDisplayStatus;
import com.example.commerce.domain.order.dto.OrderResponse;
import com.example.commerce.domain.order.facade.OrderFacade;
import com.example.commerce.domain.payment.service.PaymentService;
import com.example.commerce.domain.product.entity.Product;
import com.example.commerce.domain.product.entity.ProductCategory;
import com.example.commerce.domain.refund.dto.RefundRequest;
import com.example.commerce.domain.refund.dto.RefundResponse;
import com.example.commerce.domain.refund.entity.RefundType;
import com.example.commerce.domain.refund.service.RefundService;
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
 * 실제 리포지토리·DB(H2)로 "부분 환불을 한 번 하고 나면 더 이상 부분 환불을 못 하는" 버그가
 * 고쳐졌는지 끝까지 검증한다. RefundServiceTest는 목(mock)으로 단위 검증만 하므로,
 * OrderFacade.getOrder()가 실제로 정확한 잔여 수량·진행 상태를 돌려주는지는 여기서 확인한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RefundRepeatedPartialIntegrationTest {

    private static final int INITIAL_STOCK = 10;
    private static final long UNIT_PRICE = 10_000L;
    private static final int ORDER_QUANTITY = 3;

    @Autowired private OrderFacade orderFacade;
    @Autowired private PaymentService paymentService;
    @Autowired private RefundService refundService;
    @Autowired private EntityManager entityManager;

    private Long userId;
    private Long orderId;
    private Long paymentId;
    private Long orderItemId;

    @BeforeEach
    void setUp() {
        User user = User.of("환불자", "refund-repeat@test.com", "password", UserRole.USER);
        entityManager.persist(user);
        userId = user.getId();

        Product product = new Product(1L, "무선 이어폰", "설명",
                ProductCategory.ELECTRONICS, UNIT_PRICE, INITIAL_STOCK);
        entityManager.persist(product);

        Cart cart = new Cart(user);
        entityManager.persist(cart);
        CartItem cartItem = new CartItem(cart, product, ORDER_QUANTITY);
        entityManager.persist(cartItem);
        Long cartItemId = cartItem.getId();

        entityManager.flush();
        entityManager.clear();

        CreateOrderResponse created = orderFacade.createOrder(userId,
                new CreateOrderRequest(List.of(cartItemId), null, "refund-repeat-key", null));
        orderId = created.orderId();
        paymentId = created.paymentId();

        // 결제 승인은 주문 확정(CONFIRMED)까지 함께 묶여 있어 PaymentService를 통해야 한다.
        paymentService.approvePayment(userId, paymentId);
        flushAndClear();

        orderItemId = orderFacade.getOrder(userId, orderId).items().get(0).orderItemId();
    }

    @Test
    @DisplayName("부분 환불을 완료한 뒤에도 남은 수량으로 다시 부분 환불을 요청할 수 있다")
    void canRequestPartialRefundAgainAfterPreviousOneCompleted() {
        // 1개 환불
        requestAndCompletePartialRefund(1);
        flushAndClear();

        OrderResponse afterFirst = orderFacade.getOrder(userId, orderId);
        assertThat(afterFirst.displayStatus()).isEqualTo(OrderDisplayStatus.PARTIALLY_REFUNDED);
        assertThat(afterFirst.items().get(0).refundedQuantity()).isEqualTo(1);

        // 예전 버그였다면 여기서 REFUND_NOT_ALLOWED("이미 환불이 접수된 결제입니다")로 실패했다.
        requestAndCompletePartialRefund(1);
        flushAndClear();

        OrderResponse afterSecond = orderFacade.getOrder(userId, orderId);
        assertThat(afterSecond.displayStatus()).isEqualTo(OrderDisplayStatus.PARTIALLY_REFUNDED);
        assertThat(afterSecond.items().get(0).refundedQuantity()).isEqualTo(2);

        // 마지막 남은 1개까지 환불하면 3개 전량이 반영된다.
        requestAndCompletePartialRefund(1);
        flushAndClear();

        OrderResponse afterThird = orderFacade.getOrder(userId, orderId);
        assertThat(afterThird.items().get(0).refundedQuantity()).isEqualTo(ORDER_QUANTITY);
    }

    @Test
    @DisplayName("잔여 수량을 초과해서 부분 환불을 요청하면 거부된다")
    void rejectsPartialRefundExceedingRemainingQuantity() {
        requestAndCompletePartialRefund(2);
        flushAndClear();

        // 남은 건 1개뿐인데 2개를 요청한다.
        var request = new RefundRequest(paymentId, RefundType.PARTIAL, "초과 요청",
                List.of(new RefundRequest.Item(orderItemId, 2)));

        assertThat(catchThrowableMessage(() -> refundService.createRefund(userId, request)))
                .contains("환불 가능한 수량");
    }

    private void requestAndCompletePartialRefund(int quantity) {
        var request = new RefundRequest(paymentId, RefundType.PARTIAL, "부분 환불 테스트",
                List.of(new RefundRequest.Item(orderItemId, quantity)));
        RefundResponse refund = refundService.createRefund(userId, request);
        refundService.completeRefund(userId, refund.id());
    }

    private String catchThrowableMessage(Runnable runnable) {
        try {
            runnable.run();
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
