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
import com.example.commerce.domain.order.dto.OrderResponse;
import com.example.commerce.domain.order.dto.OrderPreviewResponse.OrderPreviewItemResponse;
import com.example.commerce.domain.order.entity.Order;
import com.example.commerce.domain.order.entity.OrderItem;
import com.example.commerce.domain.order.entity.OrderStatus;
import com.example.commerce.domain.order.service.OrderService;
import com.example.commerce.domain.payment.entity.Payment;
import com.example.commerce.domain.payment.entity.PaymentStatus;
import com.example.commerce.domain.payment.repository.PaymentRepository;
import com.example.commerce.domain.payment.service.PaymentService;
import com.example.commerce.domain.product.entity.Product;
import com.example.commerce.domain.product.entity.ProductStatus;
import com.example.commerce.domain.product.repository.ProductRepository;
import com.example.commerce.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 장바구니·상품·이벤트·쿠폰·결제를 연결하는 주문 처리의 트랜잭션 경계다.
 * 개별 도메인의 상태 검증은 엔티티와 서비스에 맡기고 실행 순서를 조정한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderFacade {

    private final OrderService orderService;
    private final CouponService couponService;
    private final PaymentRepository paymentRepository;
    private final UserService userService;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final EventRepository eventRepository;
    private final PaymentService paymentService;

    /** 재고 차감, 주문 저장, 쿠폰 예약, 결제 생성 중 하나라도 실패하면 전체를 롤백한다. */
    @Transactional
    public CreateOrderResponse createOrder(Long userId, CreateOrderRequest request) {
        // 항목을 지정하지 않으면 전체 장바구니를 주문하는 계약으로 빈 목록에 통일한다.
        List<Long> cartItemIds = request.cartItemIds() != null ? request.cartItemIds() : List.of();

        // 존재하는 사용자의 장바구니 항목만 조회하여 다른 사용자의 상품 선택을 차단한다.
        userService.findUser(userId);
        List<CartItem> cartItems = getValidatedCartItems(userId, cartItemIds);

        // 상품 정보는 잠금 조회 이후에 읽고, 여기서는 연관 상품의 ID만 추출한다.
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
            OrderItem orderItem = createOrderItem(product, cartItem.getQuantity(), events);
            // 실제 차감은 상품 도메인에 한 번 맡긴다. 여기서 이벤트 재고를 중복 차감하지 않는다.
            product.decreaseStock(orderItem.getQuantity());
            orderItems.add(orderItem);
        }

        // 결제가 참조할 주문을 먼저 저장한다. 쿠폰 예약과 결제 생성도 같은 트랜잭션이다.
        Order order = orderService.saveOrder(new Order(userId, orderItems));

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

        // 할인 적용을 마친 저장된 주문으로 READY 결제를 만들고 승인에 필요한 paymentId를 응답한다.
        Payment payment = paymentService.createPayment(order, order.getPaymentAmount());

        return CreateOrderResponse.from(order, payment);
    }

    /** 가격과 수량만 미리 계산한다. 주문 저장·재고 차감·쿠폰 예약은 수행하지 않는다. */
    public OrderPreviewResponse getOrderPreview(Long userId, List<Long> cartItemIds) {
        // 미리보기에서도 사용자 존재 여부와 장바구니 소유권을 확인한다.
        userService.findUser(userId);
        // 리스트가 비어있으면 "전체 장바구니", 값이 있으면 "선택된 상품만"
        List<CartItem> cartItems = getValidatedCartItems(
                userId, cartItemIds != null ? cartItemIds : List.of());
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
                // 저장하지 않는 임시 주문 항목으로 생성 경로와 동일한 단가·수량 검증을 재사용한다.
                OrderItem orderItem = createOrderItem(product, cartItem.getQuantity(), events);
                long subTotal = orderItem.getSubTotal();
                totalQuantity = Math.addExact(totalQuantity, orderItem.getQuantity());
                totalAmount = Math.addExact(totalAmount, subTotal);
                // 이벤트 할인이 반영된 상품 금액에는 쿠폰 할인을 중복 적용하지 않는다.
                if (!orderItem.hasAppliedEvent()) {
                    couponEligibleAmount = Math.addExact(couponEligibleAmount, subTotal);
                }

                items.add(OrderPreviewItemResponse.from(orderItem, subTotal));
            }
        } catch (ArithmeticException e) {
            // long 범위를 넘는 합계가 음수 등으로 바뀌지 않도록 주문 금액 오류로 전달한다.
            throw new BusinessException(ErrorCode.ORDER_AMOUNT_OVERFLOW);
        }

        return OrderPreviewResponse.from(
                items, totalQuantity, totalAmount, couponEligibleAmount
        );
    }

    /** 소유권을 확인한 주문에 결제 정보를 합쳐 상세 응답을 만든다. */
    public OrderResponse getOrder(Long userId, Long orderId) {
        Order order = orderService.getOwnedOrder(userId, orderId);
        // 결제가 아직 없는 기존 주문도 조회할 수 있도록 결제 정보는 선택적으로 결합한다.
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        return OrderResponse.from(order, payment);
    }

    /** 미결제 주문의 재고 복구, 결제 실패 처리, 쿠폰 예약 해제를 함께 커밋한다. */
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
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

        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId).orElse(null);
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
            payment.fail("주문이 취소되었습니다.");
        }

        if (order.getUserCouponId() != null) {
            // 쿠폰에 주문 ID가 없으므로 잠근 주문의 userCouponId를 사용하고 RESERVED만 해제한다.
            couponService.releaseCoupon(userId, order.getUserCouponId());
        }

        order.cancel();
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

    /** 이벤트 상품만 일괄 조회하고 상품당 적용 가능한 이벤트가 하나인지 확인한다. */
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

        Map<Long, Event> events = new HashMap<>();
        // 한 번의 조회에서 같은 시각을 기준으로 모든 이벤트의 적용 여부를 판단한다.
        for (Event event : eventRepository.findApplicableEvents(
                eventProductIds, EventStatus.ACTIVE, LocalDateTime.now())) {
            if (events.putIfAbsent(event.getProduct().getId(), event) != null) {
                // 중복 이벤트에서 임의의 가격을 고르지 않고 잘못된 적용 상태로 거부한다.
                throw new BusinessException(ErrorCode.ORDER_ITEM_UNAVAILABLE,
                        "상품에 적용 가능한 이벤트가 여러 개입니다.");
            }
        }
        return events;
    }

    /** 이후 상품 정보가 바뀌어도 주문 가격이 바뀌지 않도록 ID·이름·단가·수량을 복사한다. */
    private OrderItem createOrderItem(Product product, Integer quantity, Map<Long, Event> events) {
        Event event = null;
        if (product.getStatus() == ProductStatus.ON_EVENT) {
            event = events.get(product.getId());
            if (event == null) {
                // 상품 상태만 ON_EVENT여도 기간·상태 조건에 맞는 이벤트가 없으면 주문할 수 없다.
                throw new BusinessException(ErrorCode.ORDER_ITEM_UNAVAILABLE,
                        "이벤트 상품 주문은 진행 중인 이벤트 기간에만 가능합니다.");
            }
        } else if (product.getStatus() != ProductStatus.ON_SALE) {
            // 일반 판매나 유효한 이벤트 판매 이외의 상품 상태는 주문 대상에서 제외한다.
            throw new BusinessException(ErrorCode.ORDER_ITEM_UNAVAILABLE);
        }

        // eventPrice는 이미 결정된 행사 단가다. discountRate로 다시 할인하지 않는다.
        return new OrderItem(
                product.getId(),
                event == null ? null : event.getId(),
                product.getName(),
                event == null ? product.getPrice() : event.getEventPrice(),
                quantity
        );
    }

    /** 전체/선택 장바구니 조회를 통일하고 항목 누락과 빈 장바구니를 거부한다. */
    private List<CartItem> getValidatedCartItems(Long userId, List<Long> cartItemIds) {
        if (cartItemIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_CART_ITEM_IDS);
        }
        if (cartItemIds.stream().distinct().count() != cartItemIds.size()) {
            throw new BusinessException(ErrorCode.DUPLICATE_ORDER_CART_ITEM);
        }

        // 상품은 LAZY로 유지해 아래 상품 잠금 조회 전에 로딩하지 않는다.
        List<CartItem> cartItems = cartItemIds.isEmpty()
                ? cartItemRepository.findAllByCartUserId(userId)
                : cartItemRepository.findAllByCartUserIdAndIdIn(userId, cartItemIds);

        // 없는 항목이나 다른 사용자의 항목이 포함되면 일부만 주문하지 않고 거부한다.
        if (!cartItemIds.isEmpty() && cartItems.size() != cartItemIds.size()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }

        return cartItems;
    }
}
