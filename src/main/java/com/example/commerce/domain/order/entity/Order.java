package com.example.commerce.domain.order.entity;
import com.example.commerce.common.entity.BaseEntity;
import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    private static final DateTimeFormatter ORDER_NUMBER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "payment_amount", nullable = false)
    private Long paymentAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "user_coupon_id")
    private Long userCouponId;

    @Column(name = "coupon_discount_amount", nullable = false)
    private Long couponDiscountAmount;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    public Order(Long userId, List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_ITEMS_REQUIRED);
        }

        calculateTotalAmount(orderItems);

        this.userId = userId;
        this.orderNumber = generateOrderNumber();
        this.status = OrderStatus.PAYMENT_PENDING;

        orderItems.forEach(this::addOrderItem);
    }

    private void addOrderItem(OrderItem orderItem) {
        orderItem.setOrder(this);
        this.orderItems.add(orderItem);
    }

    // 외부에서 항목을 추가·삭제해 저장된 주문 금액과 항목 구성이 어긋나는 것을 막는다.
    // JPA가 관리하는 내부 목록은 유지하며 항목 객체 자체를 복제하는 것은 아니다.
    public List<OrderItem> getOrderItems() {
        return List.copyOf(orderItems);
    }

    public long getTotalQuantity() {
        return orderItems.stream()
                .mapToLong(OrderItem::getQuantity)
                .sum();
    }

    public long getCouponEligibleAmount() {
        long eligibleAmount = 0L;
        for (OrderItem item : orderItems) {
            if (!item.hasAppliedEvent()) {
                eligibleAmount = Math.addExact(eligibleAmount, item.getSubTotal());
            }
        }
        return eligibleAmount;
    }

    public void applyCoupon(Long userCouponId, Long discountAmount) {
        if (status != OrderStatus.PAYMENT_PENDING) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS, "결제 대기 중인 주문에만 쿠폰을 적용할 수 있습니다.");
        }
        if (this.userCouponId != null) {
            throw new BusinessException(ErrorCode.ORDER_COUPON_ALREADY_APPLIED);
        }
        if (userCouponId == null) {
            throw new BusinessException(ErrorCode.ORDER_COUPON_REQUIRED);
        }
        if (discountAmount == null || discountAmount < 0 || discountAmount > getCouponEligibleAmount()) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_COUPON_DISCOUNT);
        }

        this.userCouponId = userCouponId;
        this.couponDiscountAmount = discountAmount;
        this.paymentAmount = totalAmount - discountAmount;
    }

    private String generateOrderNumber() {
        return "ORD-" + LocalDateTime.now().format(ORDER_NUMBER_TIME)
                + "-" + UUID.randomUUID().toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase(Locale.ROOT);
    }

    private void calculateTotalAmount(List<OrderItem> items) {
        long total = 0L;
        Set<OrderItem> uniqueItems = new HashSet<>();

        for (OrderItem item : items) {
            if (item == null) {
                throw new BusinessException(ErrorCode.ORDER_ITEM_REQUIRED);
            }
            if (!uniqueItems.add(item)) {
                throw new BusinessException(ErrorCode.DUPLICATE_ORDER_ITEM);
            }
            if (item.getOrder() != null) {
                throw new BusinessException(ErrorCode.ORDER_ITEM_ALREADY_ASSIGNED);
            }

            try {
                long subTotal = item.getSubTotal();
                total = Math.addExact(total, subTotal);
            } catch (ArithmeticException e) {
                throw new BusinessException(ErrorCode.ORDER_AMOUNT_OVERFLOW);
            }
        }

        this.totalAmount = total;
        this.couponDiscountAmount = 0L;
        this.paymentAmount = total;
    }

    private void changeStatus(OrderStatus target) {
        if (target == null) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS, "변경할 주문 상태는 필수입니다.");
        }

        if (status != target && !status.canTransitTo(target)) {
            throw new BusinessException(
                    ErrorCode.INVALID_ORDER_STATUS,
                    "주문 상태를 " + status + "에서 " + target + "로 변경할 수 없습니다."
            );
        }

        status = target;
    }

    public void confirm() {
        changeStatus(OrderStatus.CONFIRMED);
    }

    public void cancel() {
        if (status == OrderStatus.CANCELLED) {
            return;
        }

        changeStatus(OrderStatus.CANCELLED);
        this.canceledAt = LocalDateTime.now();
    }
}
