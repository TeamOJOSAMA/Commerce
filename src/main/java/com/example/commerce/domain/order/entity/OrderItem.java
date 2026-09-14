package com.example.commerce.domain.order.entity;

import com.example.commerce.common.entity.BaseEntity;
import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;

    @Column(name = "event_id", updatable = false)
    private Long eventId;

    @Column(name = "product_name", nullable = false, updatable = false)
    private String productName;

    @Column(name = "unit_price", nullable = false, updatable = false)
    private Long unitPrice;

    @Column(nullable = false, updatable = false)
    private Integer quantity;

    // 주문 전체 쿠폰 할인액 중 이 항목에 배분된 몫이다. 이벤트 항목은 쿠폰 대상이 아니라 항상 0이다.
    // 조회 때마다 다시 계산하지 않고 Order.applyCoupon()이 한 번 정한 값을 저장한다. 화면의 항목별 실결제액과
    // 부분 환불의 환불액이 같은 값을 봐야 하고, 배분 규칙이 바뀌어도 이미 결제된 주문의 금액이 달라지면 안 된다.
    //
    // 다른 스냅샷 컬럼과 달리 updatable = false를 붙이지 않는다. 주문은 IDENTITY 전략이라 saveOrder() 시점에
    // 항목까지 INSERT되고 쿠폰 적용은 그 뒤에 오므로 배분액은 UPDATE로만 반영된다. updatable = false면
    // 그 UPDATE에서 이 컬럼이 빠져 메모리에만 값이 남고 DB에는 0이 저장된다.
    // 값의 불변성은 Order.applyCoupon()이 한 번만 호출되는 것으로 보장한다.
    @ColumnDefault("0")
    @Column(name = "coupon_discount_share", nullable = false)
    private Long couponDiscountShare;

    public OrderItem(
            Long productId,
            Long eventId,
            String productName,
            Long unitPrice,
            Integer quantity
    ) {
        if (unitPrice == null || unitPrice < 0) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_ITEM_PRICE);
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_ITEM_QUANTITY);
        }

        this.productId = productId;
        this.eventId = eventId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.couponDiscountShare = 0L;
    }

    void setOrder(Order order) {
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_REQUIRED);
        }

        if (this.order != null && this.order != order) {
            throw new BusinessException(ErrorCode.ORDER_ITEM_ALREADY_ASSIGNED);
        }

        this.order = order;
    }

    // Order.applyCoupon()만 호출한다. 배분액이 소계를 넘으면 실결제액이 음수가 되므로 여기서 막는다.
    void allocateCouponDiscount(long share) {
        if (share < 0 || share > getSubTotal()) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_COUPON_DISCOUNT,
                    "항목 쿠폰 배분액은 0 이상이며 항목 소계를 초과할 수 없습니다.");
        }

        this.couponDiscountShare = share;
    }

    public boolean hasAppliedEvent() {
        return eventId != null;
    }

    public Long getSubTotal() {
        return Math.multiplyExact(unitPrice, quantity);
    }

    // 소계에서 쿠폰 배분액을 뺀 이 항목의 실결제액이다. 부분 환불 금액 계산의 기준이다.
    // 결제 완료 여부와 무관한 주문 시점 금액이며, 환불 뒤 남은 금액을 뜻하지 않는다.
    public long getPaidAmount() {
        return getSubTotal() - couponDiscountShare;
    }
}
