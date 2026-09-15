package com.example.commerce.domain.order.entity;
import com.example.commerce.common.entity.BaseEntity;
import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

@Entity
// 유일 제약: 같은 사용자가 같은 키로 보낸 주문 요청은 DB가 한 건만 허용한다.
// 인덱스: 주문 목록은 사용자로 거른 뒤 생성 시각 역순으로 읽으므로 정렬까지 인덱스로 처리한다.
//         유일 제약의 (user_id, idempotency_key)는 user_id 검색만 돕고 created_at 정렬은 돕지 못한다.
@Table(
        name = "orders",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_orders_user_id_idempotency_key",
                columnNames = {"user_id", "idempotency_key"}),
        indexes = @Index(
                name = "idx_orders_user_id_created_at",
                columnList = "user_id, created_at"))
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

    // 기존 주문에는 값이 없을 수 있어 컬럼은 null을 허용하고, 새 주문은 생성자에서 필수로 받는다.
    @Column(name = "idempotency_key", length = 64, updatable = false)
    private String idempotencyKey;

    // 취소된 주문만 값을 갖는다. 기존 행은 null이므로 조회 쪽에서 사유 미상으로 다뤄야 한다.
    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_reason", length = 30)
    private OrderCancelReason cancelReason;

    public Order(Long userId, List<OrderItem> orderItems, String idempotencyKey) {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_ITEMS_REQUIRED);
        }
        // 키가 없으면 중복 요청을 구분할 수 없으므로 저장 전에 거부한다.
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "멱등성 키는 필수입니다.");
        }

        calculateTotalAmount(orderItems);

        this.userId = userId;
        this.orderNumber = generateOrderNumber();
        this.status = OrderStatus.PAYMENT_PENDING;
        this.idempotencyKey = idempotencyKey;

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

    // 목록과 결제창에 표시할 주문명이다. 항목이 여럿이면 "상품명 외 N건"으로 요약한다.
    public String getOrderName() {
        if (orderItems.isEmpty()) {
            return "주문";
        }

        String firstName = orderItems.get(0).getProductName();
        if (orderItems.size() == 1) {
            return firstName;
        }

        return firstName + " 외 " + (orderItems.size() - 1) + "건";
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

    /**
     * 쿠폰을 적용하고 할인액을 항목별로 배분해 {@link OrderItem}에 저장한다.
     *
     * <p>배분액은 조회 때마다 다시 계산하지 않고 이 시점에 한 번 정한다. 화면의 항목별 실결제액과
     * 부분 환불의 환불액이 같은 값을 봐야 하고, 배분 규칙이 나중에 바뀌어도 이미 결제된 주문의 금액이
     * 달라지면 안 되기 때문이다. 단가·상품명을 스냅샷으로 두는 이유와 같다.</p>
     */
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

        // 항목 배분이 실패하면 주문 금액도 바꾸지 않도록 먼저 수행한다.
        allocateCouponDiscount(discountAmount);

        this.userCouponId = userCouponId;
        this.couponDiscountAmount = discountAmount;
        this.paymentAmount = totalAmount - discountAmount;
    }

    /**
     * 쿠폰 할인액을 쿠폰 적용 대상 항목(이벤트 미적용)의 소계 비율로 나눈다.
     *
     * <p>원 미만은 버리고, 버림으로 남은 잔액은 버린 소수 부분이 큰 항목부터 1원씩 더한다(동률이면 앞 항목).
     * 잔액을 마지막 항목에 몰면 소액 항목의 배분액이 소계를 넘어 실결제액이 음수가 될 수 있어 이렇게 한다.
     * 소수 부분이 남은 항목은 소계보다 작은 값을 받은 상태라 1원을 더해도 소계를 넘지 않고,
     * 잔액은 항상 소수 부분이 남은 항목 수보다 작으므로 더할 항목이 모자라지 않는다.
     * 결과는 배분액 합계 == 할인액, 0 <= 항목 배분액 <= 항목 소계를 만족한다.</p>
     */
    private void allocateCouponDiscount(long discountAmount) {
        if (discountAmount == 0L) {
            return;
        }

        List<OrderItem> eligibleItems = orderItems.stream()
                .filter(item -> !item.hasAppliedEvent())
                .toList();
        BigInteger eligibleAmount = BigInteger.valueOf(getCouponEligibleAmount());
        BigInteger discount = BigInteger.valueOf(discountAmount);

        int size = eligibleItems.size();
        long[] shares = new long[size];
        BigInteger[] remainders = new BigInteger[size];
        long allocated = 0L;
        for (int i = 0; i < size; i++) {
            // 할인액 × 소계는 long을 넘을 수 있어 BigInteger로 계산한다.
            BigInteger[] quotientAndRemainder = discount
                    .multiply(BigInteger.valueOf(eligibleItems.get(i).getSubTotal()))
                    .divideAndRemainder(eligibleAmount);
            shares[i] = quotientAndRemainder[0].longValueExact();
            remainders[i] = quotientAndRemainder[1];
            allocated += shares[i];
        }

        // 나머지가 큰 항목 순으로 정렬하되 동률이면 원래 순서를 유지한다(정렬은 안정적이다).
        List<Integer> indexesByRemainderDesc = IntStream.range(0, size).boxed()
                .sorted(Comparator.comparing((Integer index) -> remainders[index]).reversed())
                .toList();
        long leftover = discountAmount - allocated;
        for (int rank = 0; rank < leftover; rank++) {
            shares[indexesByRemainderDesc.get(rank)]++;
        }

        for (int i = 0; i < size; i++) {
            eligibleItems.get(i).allocateCouponDiscount(shares[i]);
        }
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

    /**
     * 주문을 취소하고 경위를 함께 남긴다.
     *
     * <p>사유를 선택 인자로 두지 않는 것은 의도다. 취소 경로가 늘어날 때 사유를 빠뜨린 호출을
     * 컴파일 단계에서 잡기 위해서다. 이미 취소된 주문은 최초 취소의 시각과 사유를 유지한다.</p>
     */
    public void cancel(OrderCancelReason cancelReason) {
        if (cancelReason == null) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS, "주문 취소 사유는 필수입니다.");
        }

        if (status == OrderStatus.CANCELLED) {
            return;
        }

        changeStatus(OrderStatus.CANCELLED);
        this.canceledAt = LocalDateTime.now();
        this.cancelReason = cancelReason;
    }
}
