package com.example.commerce.domain.order.facade;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.cart.entity.Cart;
import com.example.commerce.domain.cart.entity.CartItem;
import com.example.commerce.domain.coupon.entity.Coupon;
import com.example.commerce.domain.coupon.entity.UserCoupon;
import com.example.commerce.domain.coupon.entity.UserCouponStatus;
import com.example.commerce.domain.order.dto.OrderPreviewResponse;
import com.example.commerce.domain.order.dto.OrderPreviewResponse.OrderPreviewItemResponse;
import com.example.commerce.domain.order.entity.OrderItemUnavailableReason;
import com.example.commerce.domain.product.entity.Product;
import com.example.commerce.domain.product.entity.ProductCategory;
import com.example.commerce.domain.user.entity.User;
import com.example.commerce.domain.user.entity.UserRole;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 미리보기는 구매할 수 없는 항목이 있어도 주문서를 돌려주고, 장바구니 항목 수에 비례해 조회가 늘지 않는다.
// 선택한 쿠폰의 할인액과 결제 예정 금액은 서버가 계산해 내려주며, 쿠폰 상태는 바꾸지 않는다.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderPreviewIntegrationTest {

    private static final long UNIT_PRICE = 10_000L;

    @Autowired private OrderFacade orderFacade;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("품절 상품이 섞여 있어도 주문서를 돌려주고 해당 항목만 사유와 함께 제외한다")
    void getOrderPreview_keepsPreviewWhenItemIsUnavailable() {
        User user = persistUser("preview-soldout@test.com");
        Cart cart = persistCart(user);
        Product onSale = persistProduct("판매중 상품", 10);
        Product soldout = persistProduct("품절 상품", 10);
        soldout.soldout();
        persistCartItem(cart, onSale, 2);
        persistCartItem(cart, soldout, 1);
        flushAndClear();

        OrderPreviewResponse preview = orderFacade.getOrderPreview(user.getId(), null, null);

        assertThat(preview.hasUnavailableItem()).isTrue();
        assertThat(preview.unavailableItemCount()).isEqualTo(1);
        // 합계는 주문 가능한 항목만 더한다.
        assertThat(preview.totalQuantity()).isEqualTo(2);
        assertThat(preview.totalAmount()).isEqualTo(UNIT_PRICE * 2);
        // 쿠폰 적용 대상 금액은 화면이 쿠폰 조회 API에 넘기는 값이라 구매 불가 항목이 섞이면 안 된다.
        assertThat(preview.couponEligibleAmount()).isEqualTo(UNIT_PRICE * 2);

        OrderPreviewItemResponse unavailableItem = preview.items().stream()
                .filter(item -> !item.available())
                .findFirst()
                .orElseThrow();
        assertThat(unavailableItem.productName()).isEqualTo("품절 상품");
        // 화면은 코드로 분기하고 문구는 그대로 보여준다.
        assertThat(unavailableItem.unavailableReason()).isEqualTo(OrderItemUnavailableReason.SOLD_OUT);
        assertThat(unavailableItem.unavailableReasonMessage()).isEqualTo("품절된 상품입니다.");
        // 화면이 항목을 바로 빼거나 고칠 수 있도록 장바구니 항목 ID를 함께 내려준다.
        assertThat(unavailableItem.cartItemId()).isNotNull();
    }

    @Test
    @DisplayName("재고보다 많은 수량은 남은 수량과 함께 구매 불가로 표시한다")
    void getOrderPreview_marksStockShortage() {
        User user = persistUser("preview-stock@test.com");
        Cart cart = persistCart(user);
        Product product = persistProduct("재고 부족 상품", 1);
        persistCartItem(cart, product, 3);
        flushAndClear();

        OrderPreviewResponse preview = orderFacade.getOrderPreview(user.getId(), null, null);

        OrderPreviewItemResponse item = preview.items().get(0);
        assertThat(item.available()).isFalse();
        // 남은 수량은 문구에 넣지 않고 availableStock으로 내려준다.
        assertThat(item.unavailableReason()).isEqualTo(OrderItemUnavailableReason.OUT_OF_STOCK);
        assertThat(item.unavailableReasonMessage()).isEqualTo("재고가 부족합니다.");
        assertThat(item.availableStock()).isEqualTo(1);
        assertThat(preview.totalAmount()).isZero();
    }

    @Test
    @DisplayName("장바구니 항목이 늘어도 조회 수는 그대로다")
    void getOrderPreview_doesNotGrowQueriesPerCartItem() {
        User smallCartUser = persistUser("preview-small@test.com");
        Cart smallCart = persistCart(smallCartUser);
        for (int i = 0; i < 2; i++) {
            persistCartItem(smallCart, persistProduct("상품S" + i, 10), 1);
        }

        User largeCartUser = persistUser("preview-large@test.com");
        Cart largeCart = persistCart(largeCartUser);
        for (int i = 0; i < 10; i++) {
            persistCartItem(largeCart, persistProduct("상품L" + i, 10), 1);
        }
        flushAndClear();

        long smallCartQueries = countQueries(() -> orderFacade.getOrderPreview(smallCartUser.getId(), null, null));
        long largeCartQueries = countQueries(() -> orderFacade.getOrderPreview(largeCartUser.getId(), null, null));

        // 상품을 fetch join으로 함께 읽으므로 항목 수와 무관하게 장바구니·상품 조회 1건이다.
        // fetch join이 빠지면 largeCartQueries가 항목 수만큼 늘어난다.
        assertThat(smallCartQueries).isEqualTo(1L);
        assertThat(largeCartQueries).isEqualTo(smallCartQueries);
    }

    @Test
    @DisplayName("쿠폰을 선택하면 할인액과 결제 예정 금액을 함께 내려주고 쿠폰은 예약하지 않는다")
    void getOrderPreview_appliesSelectedCoupon() {
        User user = persistUser("preview-coupon@test.com");
        Cart cart = persistCart(user);
        persistCartItem(cart, persistProduct("쿠폰 상품", 10), 2);
        Long userCouponId = persistUserCoupon(user, new Coupon("10% 할인", 10, null, null, 100));
        flushAndClear();

        OrderPreviewResponse preview = orderFacade.getOrderPreview(user.getId(), null, userCouponId);

        long eligibleAmount = UNIT_PRICE * 2;
        assertThat(preview.couponEligibleAmount()).isEqualTo(eligibleAmount);
        assertThat(preview.couponDiscountAmount()).isEqualTo(eligibleAmount / 10);
        // 화면은 이 값을 주문 생성의 expectedPaymentAmount로 그대로 보낸다.
        assertThat(preview.paymentAmount()).isEqualTo(eligibleAmount - eligibleAmount / 10);
        // 미리보기는 조회용 계산만 하므로 쿠폰이 RESERVED로 바뀌지 않는다.
        assertThat(entityManager.find(UserCoupon.class, userCouponId).getStatus())
                .isEqualTo(UserCouponStatus.AVAILABLE);
    }

    @Test
    @DisplayName("쿠폰을 선택하지 않으면 할인액은 0이고 결제 예정 금액은 총액과 같다")
    void getOrderPreview_withoutCoupon() {
        User user = persistUser("preview-no-coupon@test.com");
        Cart cart = persistCart(user);
        persistCartItem(cart, persistProduct("일반 상품", 10), 3);
        flushAndClear();

        OrderPreviewResponse preview = orderFacade.getOrderPreview(user.getId(), null, null);

        assertThat(preview.couponDiscountAmount()).isZero();
        assertThat(preview.paymentAmount()).isEqualTo(preview.totalAmount());
    }

    @Test
    @DisplayName("적용할 수 없는 쿠폰이면 주문서 대신 해당 쿠폰의 오류로 거부한다")
    void getOrderPreview_rejectsUnavailableCoupon() {
        User user = persistUser("preview-coupon-min@test.com");
        Cart cart = persistCart(user);
        persistCartItem(cart, persistProduct("소액 상품", 10), 1);
        // 최소 주문 금액이 대상 금액(10,000원)보다 크다.
        Long userCouponId = persistUserCoupon(user, new Coupon("5만원 이상 10% 할인", 10, 50_000, null, 100));
        flushAndClear();

        // 사유를 응답에 싣지 않고 쿠폰 도메인의 오류 코드를 그대로 올려보낸다. 파사드가 잡으면 rollback-only 문제가 생긴다.
        assertThatThrownBy(() -> orderFacade.getOrderPreview(user.getId(), null, userCouponId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_MINIMUM_AMOUNT_NOT_MET);
    }

    private long countQueries(Runnable action) {
        Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        action.run();
        entityManager.clear();

        return statistics.getPrepareStatementCount();
    }

    private User persistUser(String email) {
        User user = User.of("주문자", email, "password", UserRole.USER);
        entityManager.persist(user);
        return user;
    }

    private Cart persistCart(User user) {
        Cart cart = new Cart(user);
        entityManager.persist(cart);
        return cart;
    }

    private Product persistProduct(String name, int stock) {
        Product product = new Product(1L, name, "설명", ProductCategory.ELECTRONICS, UNIT_PRICE, stock);
        entityManager.persist(product);
        return product;
    }

    private void persistCartItem(Cart cart, Product product, int quantity) {
        entityManager.persist(new CartItem(cart, product, quantity));
    }

    private Long persistUserCoupon(User user, Coupon coupon) {
        entityManager.persist(coupon);
        UserCoupon userCoupon = new UserCoupon(user, coupon);
        entityManager.persist(userCoupon);
        return userCoupon.getId();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
