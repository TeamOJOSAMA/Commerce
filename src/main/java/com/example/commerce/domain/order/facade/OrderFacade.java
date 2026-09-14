package com.example.commerce.domain.order.facade;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.cart.entity.CartItem;
import com.example.commerce.domain.cart.repository.CartItemRepository;
import com.example.commerce.domain.coupon.service.CouponService;
import com.example.commerce.domain.event.entity.Event;
import com.example.commerce.domain.event.entity.EventStatus;
import com.example.commerce.domain.event.repository.EventRepository;
import com.example.commerce.domain.order.dto.CreateOrderRequest;
import com.example.commerce.domain.order.dto.CreateOrderResponse;
import com.example.commerce.domain.order.dto.OrderPreviewResponse;
import com.example.commerce.domain.order.dto.OrderPreviewResponse.OrderPreviewItemResponse;
import com.example.commerce.domain.order.dto.OrderResponse;
import com.example.commerce.domain.order.dto.OrderSummaryResponse;
import com.example.commerce.domain.order.entity.Order;
import com.example.commerce.domain.order.entity.OrderCancelReason;
import com.example.commerce.domain.order.entity.OrderItem;
import com.example.commerce.domain.order.entity.OrderItemUnavailableReason;
import com.example.commerce.domain.order.entity.OrderStatus;
import com.example.commerce.domain.order.service.OrderService;
import com.example.commerce.domain.payment.entity.Payment;
import com.example.commerce.domain.payment.entity.PaymentStatus;
import com.example.commerce.domain.payment.service.PaymentService;
import com.example.commerce.domain.refund.entity.Refund;
import com.example.commerce.domain.refund.service.RefundService;
import com.example.commerce.domain.product.entity.Product;
import com.example.commerce.domain.product.entity.ProductStatus;
import com.example.commerce.domain.product.repository.ProductRepository;
import com.example.commerce.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 장바구니·상품·이벤트·쿠폰·결제를 연결하는 주문 처리의 트랜잭션 경계다.
 * 개별 도메인의 상태 검증은 엔티티와 서비스에 맡기고 실행 순서를 조정한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderFacade {

    private final OrderService orderService;
    private final CouponService couponService;
    private final UserService userService;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final EventRepository eventRepository;
    private final PaymentService paymentService;
    private final RefundService refundService;

    /**
     * 재고 차감, 주문 저장, 쿠폰 예약, 결제 생성 중 하나라도 실패하면 전체를 롤백한다.
     * 잠금은 상품(ID 오름차순) → 사용자 쿠폰 순서다. 주문과 결제는 새로 INSERT하는 행이라 잠그지 않으며,
     * 취소·승인 경로도 상품을 쿠폰보다 먼저 잠가 서로 반대 순서로 기다리는 일이 없다.
     */
    @Transactional
    public CreateOrderResponse createOrder(Long userId, CreateOrderRequest request) {
        // 같은 키로 이미 만든 주문이 있으면 재고를 다시 차감하지 않고 그 주문을 그대로 응답한다.
        // 그 주문은 이미 취소됐거나 결제가 실패했을 수 있으므로 응답에 주문·결제 상태를 함께 실어
        // 화면이 새로 만들어진 주문과 구분할 수 있게 한다.
        Optional<Order> createdOrder =
                orderService.findOrderByIdempotencyKey(userId, request.idempotencyKey());
        if (createdOrder.isPresent()) {
            Order order = createdOrder.get();
            log.info("중복 주문 요청을 기존 주문으로 응답 - orderId: {}, status: {}",
                    order.getId(), order.getStatus());

            Payment createdPayment = paymentService.findPaymentByOrderId(order.getId()).orElse(null);
            // 재응답 대상 주문은 이미 환불까지 간 상태일 수 있어 진행 상태 계산에 환불도 필요하다.
            Refund createdRefund = createdPayment == null ? null
                    : refundService.findRefundByPaymentId(createdPayment.getId()).orElse(null);

            return CreateOrderResponse.from(order, createdPayment, createdRefund);
        }

        // JWT는 상태가 없어 탈퇴한 사용자의 토큰도 만료 전까지는 유효하다.
        // 장바구니 행이 남아 있으면 없는 사용자로도 주문이 만들어지므로 존재 여부를 확인한다.
        userService.findUser(userId);
        // 생성 요청은 항목을 반드시 담으므로 전체 장바구니로 해석되는 빈 목록이 여기까지 오지 않는다.
        List<CartItem> cartItems = getValidatedCartItems(userId, request.cartItemIds(), false);

        // 상품 정보는 잠금 조회 이후에 읽고, 여기서는 연관 상품의 ID만 추출한다.
        // 지연 로딩 프록시의 ID는 초기화 없이 읽히므로 여기서 상품 조회가 추가로 나가지 않는다.
        // 중복 ID를 제거하고 정렬해 여러 상품을 다루는 요청의 잠금 순서를 맞춘다.
        List<Long> productIds = cartItems.stream()
                .map(cartItem -> cartItem.getProduct().getId())
                .distinct()
                .sorted()
                .toList();

        // 결제 생성까지 같은 트랜잭션에서 상품 락을 유지해 재고 검증과 차감을 보호한다.
        Map<Long, Product> lockedProducts = productRepository.findAllByIdsForUpdate(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 장바구니 조회 후 상품이 누락됐더라도 남아 있는 상품만 부분 주문하지 않는다.
        if (lockedProducts.size() != productIds.size()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 미리보기 결과를 재사용하지 않고 주문 시점에 적용할 이벤트와 가격을 다시 조회한다.
        Map<Long, Event> events = getApplicableEvents(new ArrayList<>(lockedProducts.values()));
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Product product = lockedProducts.get(cartItem.getProduct().getId());
            // 장바구니 수량과 잠금 후 읽은 상품·이벤트 가격을 주문 당시 정보로 복사한다.
            // 미리보기와 같은 규칙을 쓰되, 생성에서는 구매할 수 없는 항목을 만나면 주문 자체를 거부한다.
            ItemResolution resolution = resolveOrderItem(product, cartItem.getQuantity(), events);
            if (!resolution.isAvailable()) {
                // 어떤 상품이 문제인지 화면이 바로 안내할 수 있도록 상품명을 사유에 포함한다.
                throw new BusinessException(ErrorCode.ORDER_ITEM_UNAVAILABLE,
                        product.getName() + ": " + resolution.unavailableReason().getMessage());
            }

            OrderItem orderItem = resolution.orderItem();
            // 실제 차감은 상품 도메인에 한 번 맡긴다.
            product.decreaseStock(orderItem.getQuantity());
            orderItems.add(orderItem);
        }

        // 결제가 참조할 주문을 먼저 저장한다. 쿠폰 예약과 결제 생성도 같은 트랜잭션이다.
        Order order = orderService.saveOrder(new Order(userId, orderItems, request.idempotencyKey()));

        if (request.userCouponId() != null) {
            // 일반 상품 금액만 할인 대상으로 전달한다. 쿠폰은 잠금 안에서 AVAILABLE → RESERVED가 된다.
            long discountAmount = couponService.reserveCoupon(
                    userId,
                    request.userCouponId(),
                    order.getCouponEligibleAmount()
            );
            // 주문의 userCouponId가 이후 승인·취소에서 처리할 쿠폰을 식별한다.
            order.applyCoupon(request.userCouponId(), discountAmount);
        }

        // 주문서에서 본 금액과 달라졌다면 결제를 만들지 않고 거부해 사용자가 다시 확인하게 한다.
        validateExpectedPaymentAmount(request.expectedPaymentAmount(), order.getPaymentAmount());

        // 할인 적용을 마친 저장된 주문으로 READY 결제를 만들고 승인에 필요한 paymentId를 응답한다.
        Payment payment = paymentService.createPayment(order, order.getPaymentAmount());

        // 방금 만든 주문이라 환불은 있을 수 없다.
        return CreateOrderResponse.from(order, payment, null);
    }

    /**
     * 미리보기와 주문 생성 사이에 이벤트 종료·가격 변경이 있으면 사용자가 본 금액과 달라진다.
     * 요청이 확인한 금액을 보냈을 때만 비교하며, 다르면 주문을 만들지 않고 다시 확인하게 한다.
     */
    private void validateExpectedPaymentAmount(Long expectedPaymentAmount, long paymentAmount) {
        if (expectedPaymentAmount == null || expectedPaymentAmount == paymentAmount) {
            return;
        }

        throw new BusinessException(ErrorCode.ORDER_PAYMENT_AMOUNT_MISMATCH,
                "주문서에서 확인한 금액과 다릅니다. 확인 금액: " + expectedPaymentAmount
                        + "원, 현재 금액: " + paymentAmount + "원");
    }

    /**
     * 가격·수량과 선택한 쿠폰의 할인액을 미리 계산한다. 주문 저장·재고 차감·쿠폰 예약은 수행하지 않는다.
     * 구매할 수 없는 항목이 있어도 예외로 막지 않고 항목별 사유를 담아 돌려준다.
     *
     * <p>쿠폰 할인은 쿠폰 도메인의 조회용 계산(`calculateDiscount()`)에 맡기고 결과 금액만 싣는다.
     * 적용할 수 없는 쿠폰이면 그 예외를 잡지 않고 그대로 올려보내 요청이 쿠폰의 오류 코드로 실패하게 한다.
     * 예외를 여기서 잡아 사유로 바꾸면 쿠폰 서비스의 트랜잭션 프록시가 이 트랜잭션을 rollback-only로
     * 표시해 커밋 시점에 실패하므로, 사유 판정은 쿠폰 도메인에 두고 주문서는 금액만 다룬다.
     * 실제 적용 가능 여부는 주문 생성의 {@code reserveCoupon()}이 잠금 아래 다시 검증한다.</p>
     */
    public OrderPreviewResponse getOrderPreview(Long userId, List<Long> cartItemIds, Long userCouponId) {
        // 리스트가 비어있으면 "전체 장바구니", 값이 있으면 "선택된 상품만"
        // 장바구니 조회가 userId로 스코프되어 소유권은 여기서 확인된다. 아무것도 쓰지 않는 경로라
        // 사용자 존재 여부는 따로 확인하지 않으며, 없는 사용자는 빈 장바구니로 거부된다.
        // 미리보기는 아무것도 잠그지 않으므로 상품을 fetch join으로 함께 읽어 항목 수만큼의 추가 조회를 없앤다.
        List<CartItem> cartItems = getValidatedCartItems(
                userId, cartItemIds != null ? cartItemIds : List.of(), true);
        // 생성과 같은 이벤트·판매 상태 규칙을 적용하되 조회 잠금으로 재고를 확보하지는 않는다.
        Map<Long, Event> events = getApplicableEvents(cartItems.stream()
                .map(CartItem::getProduct)
                .toList());

        List<OrderPreviewItemResponse> items = new ArrayList<>();
        long totalQuantity = 0L;
        long totalAmount = 0L;
        long couponEligibleAmount = 0L;

        try {
            for (CartItem cartItem : cartItems) {
                Product product = cartItem.getProduct();
                int quantity = cartItem.getQuantity();
                // 저장하지 않는 임시 주문 항목으로 생성 경로와 동일한 단가·수량 규칙을 재사용한다.
                ItemResolution resolution = resolveOrderItem(product, quantity, events);

                // 재고 부족은 생성 경로에서 잠금 아래 decreaseStock()이 최종 판정하지만,
                // 주문서까지 진행한 뒤 실패하지 않도록 미리보기에서도 남은 수량과 함께 미리 알린다.
                OrderItemUnavailableReason unavailableReason =
                        resolution.isAvailable() && product.getStock() < quantity
                                ? OrderItemUnavailableReason.OUT_OF_STOCK
                                : resolution.unavailableReason();

                if (unavailableReason != null) {
                    items.add(OrderPreviewItemResponse.unavailable(
                            cartItem.getId(), product.getId(), product.getName(),
                            product.getPrice(), quantity, product.getStock(), unavailableReason));
                    continue;
                }

                OrderItem orderItem = resolution.orderItem();
                long subTotal = orderItem.getSubTotal();
                totalQuantity = Math.addExact(totalQuantity, orderItem.getQuantity());
                totalAmount = Math.addExact(totalAmount, subTotal);
                // 이벤트 할인이 반영된 상품 금액에는 쿠폰 할인을 중복 적용하지 않는다.
                if (!orderItem.hasAppliedEvent()) {
                    couponEligibleAmount = Math.addExact(couponEligibleAmount, subTotal);
                }

                items.add(OrderPreviewItemResponse.available(
                        cartItem.getId(), orderItem.getProductId(), orderItem.getEventId(),
                        orderItem.getProductName(), orderItem.getUnitPrice(),
                        orderItem.getQuantity(), product.getStock()));
            }
        } catch (ArithmeticException e) {
            // long 범위를 넘는 합계가 음수 등으로 바뀌지 않도록 주문 금액 오류로 전달한다.
            throw new BusinessException(ErrorCode.ORDER_AMOUNT_OVERFLOW);
        }

        // 쿠폰은 주문 가능한 항목의 대상 금액으로 계산한다. 선택하지 않았으면 할인 없이 총액이 결제 예정 금액이다.
        // 양수가 아닌 쿠폰 ID는 형식 오류라 쿠폰 서비스가 INVALID_INPUT으로 거부한다.
        long couponDiscountAmount = userCouponId == null
                ? 0L
                : couponService.calculateDiscount(userId, userCouponId, couponEligibleAmount);

        return OrderPreviewResponse.of(items, totalQuantity, totalAmount, couponEligibleAmount, couponDiscountAmount);
    }

    /**
     * 주문 목록에 진행 상태를 합쳐 반환한다.
     * 주문마다 따로 읽지 않도록 결제는 주문 ID로, 환불은 결제 ID로 각각 한 번에 조회한다.
     */
    public Page<OrderSummaryResponse> getOrders(Long userId, Pageable pageable) {
        Page<Order> orders = orderService.getOrders(userId, pageable);
        Map<Long, Payment> payments = paymentService.findPaymentsByOrderIds(
                orders.getContent().stream()
                        .map(Order::getId)
                        .toList());
        Map<Long, Refund> refunds = refundService.findRefundsByPaymentIds(
                payments.values().stream()
                        .map(Payment::getId)
                        .toList());

        // map은 전체 건수와 페이지 정보를 유지한 채 내용만 변환한다.
        return orders.map(order -> {
            Payment payment = payments.get(order.getId());
            return OrderSummaryResponse.from(order, payment,
                    payment == null ? null : refunds.get(payment.getId()));
        });
    }

    /** 소유권을 확인한 주문에 결제·환불을 합쳐 상세 응답을 만든다. */
    public OrderResponse getOrder(Long userId, Long orderId) {
        Order order = orderService.getOwnedOrder(userId, orderId);
        // 주문 생성과 결제 생성은 같은 트랜잭션이라 정상 흐름에서는 결제가 항상 존재한다.
        // 없다면 정합성이 깨진 데이터이므로 조회를 막는 대신 로그로 드러낸다.
        Payment payment = paymentService.findPaymentByOrderId(orderId).orElse(null);
        if (payment == null) {
            log.warn("결제 없는 주문 상세 조회 - orderId: {}", orderId);
        }
        // 환불은 결제에 달려 있으므로 결제가 없으면 조회하지 않는다.
        Refund refund = payment == null ? null
                : refundService.findRefundByPaymentId(payment.getId()).orElse(null);

        return OrderResponse.from(order, payment, refund);
    }

    /** 미결제 주문의 재고 복구, 결제 실패 처리, 쿠폰 예약 해제를 함께 커밋한다. */
    @Transactional
    public void cancelOrder(Long userId, Long orderId, OrderCancelReason cancelReason) {
        // 주문 → 결제 → 상품(ID 오름차순) → 쿠폰 순서로 잠근다.
        Order order = orderService.getOwnedOrderForUpdate(userId, orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            // 최초 취소 시각을 유지하고 재사용된 쿠폰의 예약까지 다시 해제하지 않도록 즉시 반환한다.
            return;
        }
        // CONFIRMED 주문의 취소는 환불 흐름이므로 일반 취소 API에서는 허용하지 않는다.
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS, "결제 대기 중인 주문만 취소할 수 있습니다.");
        }

        // 주문 생성 트랜잭션이 결제까지 함께 만들므로 결제 없는 주문은 생기지 않는다.
        // 덕분에 이 조회는 유니크 인덱스에 정확히 일치하는 행을 잠가 MySQL에서도 갭 락이 아닌 행 잠금만 잡는다.
        // 결제 행을 지우는 변경(실패 후 재결제 등)을 넣으면 빈 구간을 잠그게 되므로 이 조회부터 다시 봐야 한다.
        Payment payment = paymentService.findPaymentByOrderIdForUpdate(orderId).orElse(null);
        if (payment == null) {
            // 정합성이 깨진 데이터다. 재고와 쿠폰은 돌려줘야 하므로 취소는 계속하고 로그로만 드러낸다.
            log.warn("결제 없는 주문 취소 - orderId: {}", orderId);
        }
        // 기존 데이터에서도 결제 완료 건을 일반 취소하지 않도록 결제 상태를 확인한다.
        if (payment != null
                && payment.getStatus() != PaymentStatus.READY
                && payment.getStatus() != PaymentStatus.FAILED) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS, "결제된 주문은 취소할 수 없습니다.");
        }

        // 생성과 같이 상품을 쿠폰보다 먼저 잠가 서로 반대 순서로 기다리는 것을 피한다.
        restoreOrderStock(order);

        if (payment != null && payment.getStatus() == PaymentStatus.READY) {
            // 승인 전 사용자 취소는 FAILED로 구분한다. 이미 FAILED인 결제의 원래 사유는 유지한다.
            payment.fail(cancelReason.getMessage());
        }

        if (order.getUserCouponId() != null) {
            // 쿠폰에 주문 ID가 없으므로 잠근 주문의 userCouponId를 사용하고 RESERVED만 해제한다.
            couponService.releaseCoupon(userId, order.getUserCouponId());
        }

        // 주문 상태는 CANCELLED 하나뿐이라 사용자 취소와 결제 실패를 사유로만 구분할 수 있다.
        order.cancel(cancelReason);
    }

    private void restoreOrderStock(Order order) {
        List<OrderItem> orderItems = order.getOrderItems();
        List<Long> productIds = orderItems.stream()
                .map(OrderItem::getProductId)
                .distinct()
                .sorted()
                .toList();

        Map<Long, Product> lockedProducts = productRepository.findAllByIdsForUpdate(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        // 하나라도 삭제되었으면 일부 재고만 복구한 취소가 남지 않도록 전체 취소를 거부한다.
        if (lockedProducts.size() != productIds.size()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 일반·이벤트 상품 모두 공통 재고를 사용한다. 현재 이벤트나 장바구니는 다시 조회하지 않는다.
        for (OrderItem orderItem : orderItems) {
            Product product = lockedProducts.get(orderItem.getProductId());
            // 상품 재고는 int이므로 복구 결과가 음수로 넘치기 전에 취소를 거부한다.
            if ((long) product.getStock() + orderItem.getQuantity() > Integer.MAX_VALUE) {
                throw new BusinessException(ErrorCode.STOCK_AMOUNT_OVERFLOW);
            }
            product.restoreStock(orderItem.getQuantity());
        }
    }

    /**
     * 이벤트 상품만 일괄 조회해 상품 ID별 적용 이벤트를 돌려준다.
     * 상품당 진행 중 이벤트는 이벤트 도메인이 하나로 보장한다. 그 전제가 깨져 같은 상품에 두 건이 오면
     * 임의의 단가를 고르는 대신 toMap의 중복 키 예외로 드러난다.
     */
    private Map<Long, Event> getApplicableEvents(List<Product> products) {
        List<Long> eventProductIds = products.stream()
                .filter(product -> product.getStatus() == ProductStatus.ON_EVENT)
                .map(Product::getId)
                .distinct()
                .sorted()
                .toList();
        if (eventProductIds.isEmpty()) {
            // 일반 상품만 있는 주문은 이벤트 조회가 필요 없다.
            return Map.of();
        }

        // 한 번의 조회에서 같은 시각을 기준으로 모든 이벤트의 적용 여부를 판단한다.
        return eventRepository.findApplicableEvents(eventProductIds, EventStatus.ACTIVE, LocalDateTime.now())
                .stream()
                .collect(Collectors.toMap(
                        event -> event.getProduct().getId(),
                        Function.identity()
                ));
    }

    /**
     * 상품 상태와 적용 이벤트로 주문 항목을 만든다. 생성과 미리보기가 같은 규칙을 쓰도록 한 곳에 모았다.
     * 구매할 수 없는 상품은 예외 대신 사유를 담은 결과로 돌려주고, 거부 여부는 호출자가 결정한다.
     * 이후 상품 정보가 바뀌어도 주문 가격이 바뀌지 않도록 ID·이름·단가·수량을 복사한다.
     */
    private ItemResolution resolveOrderItem(Product product, Integer quantity, Map<Long, Event> events) {
        Event event = null;
        if (product.getStatus() == ProductStatus.ON_EVENT) {
            event = events.get(product.getId());
            if (event == null) {
                // 상품 상태만 ON_EVENT여도 기간·상태 조건에 맞는 이벤트가 없으면 주문할 수 없다.
                return ItemResolution.unavailable(OrderItemUnavailableReason.EVENT_NOT_IN_PROGRESS);
            }
        } else if (product.getStatus() != ProductStatus.ON_SALE) {
            // 일반 판매나 유효한 이벤트 판매 이외의 상품 상태는 주문 대상에서 제외한다.
            return ItemResolution.unavailable(product.getStatus() == ProductStatus.SOLDOUT
                    ? OrderItemUnavailableReason.SOLD_OUT
                    : OrderItemUnavailableReason.NOT_ON_SALE);
        }

        // eventPrice는 이미 결정된 행사 단가다. discountRate로 다시 할인하지 않는다.
        return ItemResolution.available(new OrderItem(
                product.getId(),
                event == null ? null : event.getId(),
                product.getName(),
                event == null ? product.getPrice() : event.getEventPrice(),
                quantity
        ));
    }

    /**
     * 전체/선택 장바구니 조회를 통일하고 항목 누락과 빈 장바구니를 거부한다.
     * withProduct가 true면 상품을 함께 읽는다. 잠금 조회 전에 상품을 로딩하면 안 되는 주문 생성은 false다.
     */
    private List<CartItem> getValidatedCartItems(Long userId, List<Long> cartItemIds, boolean withProduct) {
        // ID의 양수 여부와 개수 상한은 요청 형식의 문제라 생성 DTO의 Bean Validation이 맡는다.
        // 같은 항목이 두 번 오면 거부하지 않고 한 번으로 본다. 항목의 주문 수량은 장바구니 수량이 정한다.
        List<Long> distinctCartItemIds = cartItemIds.stream().distinct().toList();
        List<CartItem> cartItems = findCartItems(userId, distinctCartItemIds, withProduct);

        // 없는 항목이나 다른 사용자의 항목이 포함되면 일부만 주문하지 않고 거부한다.
        if (!distinctCartItemIds.isEmpty() && cartItems.size() != distinctCartItemIds.size()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }

        return cartItems;
    }

    private List<CartItem> findCartItems(Long userId, List<Long> cartItemIds, boolean withProduct) {
        if (cartItemIds.isEmpty()) {
            return withProduct
                    ? cartItemRepository.findAllByCartUserIdWithProduct(userId)
                    : cartItemRepository.findAllByCartUserId(userId);
        }

        return withProduct
                ? cartItemRepository.findAllByCartUserIdAndIdInWithProduct(userId, cartItemIds)
                : cartItemRepository.findAllByCartUserIdAndIdIn(userId, cartItemIds);
    }

    /** 주문 항목을 만들 수 있으면 orderItem이, 만들 수 없으면 사유가 채워진다. */
    private record ItemResolution(OrderItem orderItem, OrderItemUnavailableReason unavailableReason) {

        private static ItemResolution available(OrderItem orderItem) {
            return new ItemResolution(orderItem, null);
        }

        private static ItemResolution unavailable(OrderItemUnavailableReason unavailableReason) {
            return new ItemResolution(null, unavailableReason);
        }

        private boolean isAvailable() {
            return orderItem != null;
        }
    }
}
