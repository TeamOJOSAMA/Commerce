package com.example.commerce.domain.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

// 사용자·상품 가격·수량은 서버에서 읽고, 요청에는 선택한 장바구니 항목과 발급 쿠폰 ID만 받는다.
public record CreateOrderRequest(
        // 주문할 항목은 반드시 명시한다. 빈 목록을 전체 장바구니로 해석하면
        // 클라이언트가 잘못 보낸 빈 배열에 선택하지 않은 항목까지 결제된다.
        // 전체 장바구니 금액 확인은 항목 없이 조회할 수 있는 미리보기가 담당한다.
        @NotEmpty(message = "주문할 장바구니 항목을 선택해 주세요.")
        List<@NotNull @Positive Long> cartItemIds,
        // 원본 쿠폰이 아닌 사용자에게 발급된 쿠폰의 ID이며, null이면 쿠폰을 적용하지 않는다.
        @Positive Long userCouponId,
        // 같은 요청이 두 번 도착해도 주문이 하나만 생성되도록 클라이언트가 주문 시도마다 새로 만드는 키다.
        @NotBlank(message = "멱등성 키는 필수입니다.")
        @Size(max = 64, message = "멱등성 키는 64자를 넘을 수 없습니다.")
        String idempotencyKey,
        // 사용자가 주문서에서 확인한 결제 예정 금액이다. null이면 금액 검증을 건너뛴다.
        @PositiveOrZero Long expectedPaymentAmount
) {
    // null은 검증 전에 빈 목록으로 맞춰 이후 계산이 null을 다루지 않게 하고, 요청 이후 변경되지 않도록 복사한다.
    // 빈 목록은 위의 @NotEmpty가 거부한다.
    public CreateOrderRequest {
        cartItemIds = cartItemIds == null ? List.of() : List.copyOf(cartItemIds);
    }
}
