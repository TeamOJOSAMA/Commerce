package com.example.commerce.domain.order.dto;

import com.example.commerce.domain.order.entity.OrderItemUnavailableReason;

import java.util.List;

/**
 * 아직 저장하지 않은 주문의 예상 금액이다. 실제 주문 생성 시 가격과 이벤트를 다시 확인한다.
 *
 * <p>구매할 수 없는 항목이 섞여 있어도 주문서 전체를 실패시키지 않는다. 장바구니에는 품절되거나
 * 판매가 중지된 상품이 흔히 남아 있는데, 통째로 거부하면 사용자는 주문서를 열지도 못한 채
 * 원인을 찾아 항목을 하나씩 지워봐야 한다. 대신 항목마다 주문 가능 여부와 사유를 내려주고
 * 합계에서는 제외한다.</p>
 *
 * <p>선택한 쿠폰의 할인액과 결제 예정 금액은 서버가 계산해 내려준다. 화면은 할인 규칙을 다시
 * 구현하지 않고 paymentAmount를 주문 생성의 expectedPaymentAmount로 그대로 보낸다. 쿠폰 사용 가능
 * 여부의 판정과 사유는 쿠폰 도메인의 몫이라 응답에 싣지 않으며, 적용할 수 없는 쿠폰은 요청 자체가
 * 쿠폰의 오류 코드로 거부된다.</p>
 */
public record OrderPreviewResponse(
        List<OrderPreviewItemResponse> items,
        // 아래 합계는 모두 "주문 가능한 항목"만 더한 값이다. 구매 불가 항목은 제외한다.
        long totalQuantity,
        // 상품 단가에 이벤트 가격은 반영되어 있지만 쿠폰 할인은 적용하기 전의 합계다.
        long totalAmount,
        // 이벤트가 적용되지 않은 일반 상품 금액만 합산한 쿠폰 할인 계산 기준이다.
        // 쿠폰 도메인은 장바구니·상품·이벤트를 보지 않으므로 이 값은 주문 쪽만 계산할 수 있다.
        long couponEligibleAmount,
        // 선택한 쿠폰의 할인액이다. 쿠폰을 선택하지 않았으면 0이다.
        long couponDiscountAmount,
        // totalAmount에서 couponDiscountAmount를 뺀 결제 예정 금액이다. 주문 생성의 expectedPaymentAmount로 보낸다.
        long paymentAmount,
        // 하나라도 구매할 수 없는 항목이 있으면 true다. 화면은 주문 버튼을 막고 해당 항목을 표시한다.
        boolean hasUnavailableItem,
        int unavailableItemCount
) {
    // 호출자가 원래 목록을 수정해도 이미 만든 응답의 항목 구성이 달라지지 않도록 복사한다.
    public OrderPreviewResponse {
        items = List.copyOf(items);
    }

    public static OrderPreviewResponse of(
            List<OrderPreviewItemResponse> items,
            long totalQuantity,
            long totalAmount,
            long couponEligibleAmount,
            long couponDiscountAmount
    ) {
        int unavailableItemCount = (int) items.stream()
                .filter(item -> !item.available())
                .count();

        // 할인액은 쿠폰 적용 대상 금액을 넘지 않으므로 결제 예정 금액은 음수가 되지 않는다.
        return new OrderPreviewResponse(
                items,
                totalQuantity,
                totalAmount,
                couponEligibleAmount,
                couponDiscountAmount,
                totalAmount - couponDiscountAmount,
                unavailableItemCount > 0,
                unavailableItemCount
        );
    }

    // 저장 전 항목이므로 orderItemId 대신 장바구니 항목 ID와 상품·이벤트, 예상 단가·수량을 제공한다.
    public record OrderPreviewItemResponse(
            // 화면에서 항목을 빼거나 수량을 고칠 때 필요한 장바구니 항목 ID다.
            Long cartItemId,
            Long productId,
            // 일반 상품은 null이며, 이벤트 상품은 적용 예정 이벤트의 ID다.
            Long eventId,
            String productName,
            long unitPrice,
            int quantity,
            // 단가 × 수량이다. 구매할 수 없는 항목도 화면 표시를 위해 계산하지만 합계에는 넣지 않는다.
            long subTotal,
            // false면 이 항목 때문에 주문을 만들 수 없다. 합계에서도 제외된다.
            boolean available,
            // 주문할 수 없는 이유 코드다. 화면은 이 값으로 분기한다. 주문 가능하면 null이다.
            OrderItemUnavailableReason unavailableReason,
            // 위 코드의 안내 문구다. 화면이 그대로 보여줄 수 있게 함께 내려준다. 주문 가능하면 null이다.
            String unavailableReasonMessage,
            // 현재 남은 재고다. 수량이 재고를 넘을 때 화면이 바로 줄여줄 수 있도록 함께 내려준다.
            int availableStock
    ) {
        public static OrderPreviewItemResponse available(
                Long cartItemId, Long productId, Long eventId, String productName,
                long unitPrice, int quantity, int availableStock
        ) {
            return new OrderPreviewItemResponse(
                    cartItemId, productId, eventId, productName, unitPrice, quantity,
                    Math.multiplyExact(unitPrice, quantity), true, null, null, availableStock);
        }

        public static OrderPreviewItemResponse unavailable(
                Long cartItemId, Long productId, String productName,
                long unitPrice, int quantity, int availableStock, OrderItemUnavailableReason unavailableReason
        ) {
            return new OrderPreviewItemResponse(
                    cartItemId, productId, null, productName, unitPrice, quantity,
                    Math.multiplyExact(unitPrice, quantity), false,
                    unavailableReason, unavailableReason.getMessage(), availableStock);
        }
    }
}
