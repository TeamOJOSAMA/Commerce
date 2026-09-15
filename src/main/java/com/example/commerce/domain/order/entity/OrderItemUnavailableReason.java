package com.example.commerce.domain.order.entity;

/**
 * 장바구니 항목을 주문 항목으로 만들 수 없는 이유다. 미리보기는 항목마다 이 값을 내려주고,
 * 주문 생성은 같은 값의 문구를 붙여 요청을 거부한다. 두 경로가 같은 enum을 쓰므로 사유가 어긋나지 않는다.
 *
 * <p>문구는 표현이고 규칙이 아니므로 파사드가 아니라 여기서 갖는다. 화면은 코드로 분기하고
 * 문구는 그대로 보여주면 된다. 상품·이벤트 도메인이 자기 사유를 직접 돌려주게 되면 그 값을 이 enum으로
 * 매핑하는 것으로 충분하다.</p>
 */
public enum OrderItemUnavailableReason {

    // 상품 상태가 SOLDOUT이다. 재고 수와 무관하게 판매자가 품절로 둔 상품이다.
    SOLD_OUT("품절된 상품입니다."),
    // 판매 중이지만 장바구니 수량이 남은 재고를 넘는다. 남은 수량은 응답의 availableStock으로 함께 내려준다.
    // 미리보기의 사전 안내이며, 최종 판정은 주문 생성이 잠금 아래 decreaseStock()으로 한다.
    OUT_OF_STOCK("재고가 부족합니다."),
    // 진행 중 이벤트가 없는 ON_EVENT 상품은 구매 불가가 아니라 정가로 주문하므로 그 사유는 두지 않는다(2026-09-15 제거).
    // ON_SALE·ON_EVENT·SOLDOUT 어디에도 해당하지 않는 상태다. 현재 ProductStatus에는 그런 값이 없어
    // 상태가 비어 있는 행이나 앞으로 추가될 상태(판매 중지 등)를 받아내는 기본 사유다.
    NOT_ON_SALE("판매 중인 상품이 아닙니다.");

    private final String message;

    OrderItemUnavailableReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
