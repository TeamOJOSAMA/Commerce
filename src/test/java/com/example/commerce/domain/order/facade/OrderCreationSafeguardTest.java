package com.example.commerce.domain.order.facade;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.cart.entity.Cart;
import com.example.commerce.domain.cart.entity.CartItem;
import com.example.commerce.domain.order.dto.CreateOrderRequest;
import com.example.commerce.domain.order.dto.CreateOrderResponse;
import com.example.commerce.domain.order.dto.OrderDisplayStatus;
import com.example.commerce.domain.order.entity.Order;
import com.example.commerce.domain.order.entity.OrderCancelReason;
import com.example.commerce.domain.order.entity.OrderStatus;
import com.example.commerce.domain.order.repository.OrderRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 주문 생성 요청의 안전장치가 중복 주문과 금액 변경을 실제로 막는지 확인한다.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderCreationSafeguardTest {

    private static final int INITIAL_STOCK = 10;
    private static final long UNIT_PRICE = 50_000L;
    private static final int ORDER_QUANTITY = 2;

    @Autowired private OrderFacade orderFacade;
    @Autowired private OrderRepository orderRepository;
    @Autowired private EntityManager entityManager;

    private Long userId;
    private Long productId;
    private Long cartItemId;

    @BeforeEach
    void setUp() {
        User user = User.of("주문자", "safeguard@test.com", "password", UserRole.USER);
        entityManager.persist(user);
        userId = user.getId();

        Product product = new Product(1L, "기계식 키보드", "설명",
                ProductCategory.ELECTRONICS, UNIT_PRICE, INITIAL_STOCK);
        entityManager.persist(product);
        productId = product.getId();

        Cart cart = new Cart(user);
        entityManager.persist(cart);
        CartItem cartItem = new CartItem(cart, product, ORDER_QUANTITY);
        entityManager.persist(cartItem);
        cartItemId = cartItem.getId();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("같은 멱등성 키로 다시 요청하면 주문을 새로 만들지 않고 기존 주문을 돌려준다")
    void createOrder_sameIdempotencyKeyReturnsSameOrder() {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(cartItemId), null, "checkout-key-1", null);

        CreateOrderResponse first = orderFacade.createOrder(userId, request);
        CreateOrderResponse second = orderFacade.createOrder(userId, request);

        assertThat(second.orderId()).isEqualTo(first.orderId());
        assertThat(second.orderNumber()).isEqualTo(first.orderNumber());
        assertThat(orderRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("중복 요청은 재고를 다시 차감하지 않는다")
    void createOrder_duplicateRequestDoesNotDecreaseStockTwice() {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(cartItemId), null, "checkout-key-2", null);

        orderFacade.createOrder(userId, request);
        orderFacade.createOrder(userId, request);

        entityManager.flush();
        entityManager.clear();
        Product product = entityManager.find(Product.class, productId);
        assertThat(product.getStock()).isEqualTo(INITIAL_STOCK - ORDER_QUANTITY);
    }

    @Test
    @DisplayName("키가 다르면 같은 장바구니라도 별개의 주문으로 생성된다")
    void createOrder_differentKeyCreatesNewOrder() {
        CreateOrderResponse first = orderFacade.createOrder(
                userId, new CreateOrderRequest(List.of(cartItemId), null, "checkout-key-3", null));
        CreateOrderResponse second = orderFacade.createOrder(
                userId, new CreateOrderRequest(List.of(cartItemId), null, "checkout-key-4", null));

        assertThat(second.orderId()).isNotEqualTo(first.orderId());
        assertThat(orderRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("주문서에서 확인한 금액과 다르면 주문을 만들지 않고 거부한다")
    void createOrder_rejectsChangedPaymentAmount() {
        long wrongAmount = UNIT_PRICE * ORDER_QUANTITY - 1_000L;
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(cartItemId), null, "checkout-key-5", wrongAmount);

        assertThatThrownBy(() -> orderFacade.createOrder(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_PAYMENT_AMOUNT_MISMATCH);
    }

    @Test
    @DisplayName("확인한 금액이 현재 금액과 같으면 그대로 주문한다")
    void createOrder_acceptsMatchingPaymentAmount() {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(cartItemId), null, "checkout-key-6", UNIT_PRICE * ORDER_QUANTITY);

        CreateOrderResponse response = orderFacade.createOrder(userId, request);

        assertThat(response.paymentAmount()).isEqualTo(UNIT_PRICE * ORDER_QUANTITY);
        Order order = orderRepository.findById(response.orderId()).orElseThrow();
        assertThat(order.getOrderName()).isEqualTo("기계식 키보드");
    }

    @Test
    @DisplayName("취소된 주문의 멱등성 키로 다시 요청하면 취소 상태를 그대로 알려 준다")
    void createOrder_replayOfCancelledOrderExposesCancelledStatus() {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(cartItemId), null, "checkout-key-7", null);
        CreateOrderResponse created = orderFacade.createOrder(userId, request);
        orderFacade.cancelOrder(userId, created.orderId(), OrderCancelReason.PAYMENT_FAILED);
        entityManager.flush();
        entityManager.clear();

        CreateOrderResponse replayed = orderFacade.createOrder(userId, request);

        // 상태를 내려주지 않으면 화면은 취소된 주문에 결제창을 띄우고 승인에서야 실패한다.
        assertThat(replayed.orderId()).isEqualTo(created.orderId());
        // 결제 실패로 취소된 주문은 사용자 취소와 구분되어 내려간다.
        assertThat(replayed.displayStatus()).isEqualTo(OrderDisplayStatus.PAYMENT_FAILED);
    }

    @Test
    @DisplayName("생성 응답은 결제창에 필요한 주문명과 금액을 함께 내려 준다")
    void createOrder_responseCarriesCheckoutInformation() {
        CreateOrderResponse response = orderFacade.createOrder(
                userId, new CreateOrderRequest(List.of(cartItemId), null, "checkout-key-8", null));

        assertThat(response.orderName()).isEqualTo("기계식 키보드");
        assertThat(response.displayStatus()).isEqualTo(OrderDisplayStatus.PAYMENT_PENDING);
        assertThat(response.totalQuantity()).isEqualTo(ORDER_QUANTITY);
        assertThat(response.totalAmount()).isEqualTo(UNIT_PRICE * ORDER_QUANTITY);
        assertThat(response.couponDiscountAmount()).isZero();
        // 승인·실패 API를 호출할 진입 키는 파생할 수 없으므로 그대로 내려준다.
        assertThat(response.paymentId()).isNotNull();
    }

    @Test
    @DisplayName("구매할 수 없는 상품은 어떤 상품인지 알 수 있는 사유로 거부한다")
    void createOrder_rejectsUnavailableItemWithProductName() {
        entityManager.find(Product.class, productId).soldout();
        entityManager.flush();
        entityManager.clear();

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(cartItemId), null, "checkout-key-9", null);

        assertThatThrownBy(() -> orderFacade.createOrder(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_ITEM_UNAVAILABLE)
                .hasMessageContaining("기계식 키보드")
                .hasMessageContaining("품절");
    }
}
