package com.example.commerce.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 입력값입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_002","인증이 필요합니다."),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "COMMON_003","접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_004","서버 오류가 발생했습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_005","요청한 리소스를 찾을 수 없습니다."),

    // 유저
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "USER_001","이미 가입된 이메일입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_002","회원을 찾을 수 없습니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "USER_003","이메일 또는 비밀번호가 일치하지 않습니다."),
    LAST_ADMIN_CANNOT_BE_DEMOTED(HttpStatus.BAD_REQUEST, "USER_004","마지막 관리자는 권한을 변경할 수 없습니다."),


    // 상품
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_001","상품을 찾을 수 없습니다."),
    OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "PRODUCT_002","상품의 재고가 부족합니다."),
    INVALID_PRODUCT_STATUS(HttpStatus.NOT_ACCEPTABLE, "PRODUCT_003","유호하지 않은 상태입니다"),
    NOT_SUFFICIENT_AMOUNT(HttpStatus.BAD_REQUEST,"PRODUCT_4","최소 가격은 0 이상이여야 합니다"),
    PRICE_ERROR(HttpStatus.BAD_REQUEST,"PRODUCT_5","최소 가격이 최대 가격보다 클수 없습니다"),
    STOCK_AMOUNT_OVERFLOW(HttpStatus.BAD_REQUEST, "PRODUCT_006", "재고 수량이 허용 범위를 초과했습니다."),
    INVALID_EVENT_STOCK(HttpStatus.BAD_REQUEST, "PRODUCT_007", "이벤트 재고가 설정되지 않았습니다."),

    // 장바구니
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "CART_001","장바구니 상품을 찾을 수 없습니다."),
    CART_EMPTY(HttpStatus.NOT_FOUND, "CART_002", "장바구니가 비어있습니다."),

    // 쿠폰
    USER_COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "COUPON_001", "사용자의 발급 쿠폰을 찾을 수 없습니다."),
    USER_COUPON_UNAVAILABLE(HttpStatus.BAD_REQUEST, "COUPON_002", "사용 가능한 상태의 쿠폰이 아닙니다."),
    USER_COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "COUPON_003", "사용 기간이 만료된 쿠폰입니다."),
    COUPON_INACTIVE(HttpStatus.BAD_REQUEST, "COUPON_004", "활성 상태의 쿠폰이 아닙니다."),
    COUPON_MINIMUM_AMOUNT_NOT_MET(HttpStatus.BAD_REQUEST, "COUPON_005", "쿠폰 적용 대상 금액이 최소 주문 금액보다 적습니다."),
    INVALID_COUPON_POLICY(HttpStatus.BAD_REQUEST, "COUPON_006", "쿠폰 할인 정책이 올바르지 않습니다."),
    INVALID_COUPON_ELIGIBLE_AMOUNT(HttpStatus.BAD_REQUEST, "COUPON_007", "쿠폰 적용 대상 금액은 0 이상이어야 합니다."),

    // 주문
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_001","주문을 찾을 수 없습니다."),
    ALREADY_CANCELED(HttpStatus.BAD_REQUEST, "ORDER_002","이미 취소된 주문입니다."),
    ORDER_ITEMS_REQUIRED(HttpStatus.BAD_REQUEST, "ORDER_003", "주문 항목은 하나 이상 필요합니다."),
    ORDER_ITEM_REQUIRED(HttpStatus.BAD_REQUEST, "ORDER_004", "주문 항목은 필수입니다."),
    DUPLICATE_ORDER_ITEM(HttpStatus.BAD_REQUEST, "ORDER_005", "동일한 주문 항목을 중복 등록할 수 없습니다."),
    ORDER_ITEM_ALREADY_ASSIGNED(HttpStatus.BAD_REQUEST, "ORDER_006", "이미 주문에 속한 항목입니다."),
    ORDER_AMOUNT_OVERFLOW(HttpStatus.BAD_REQUEST, "ORDER_007", "주문 금액이 허용 범위를 초과했습니다."),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "ORDER_008", "처리할 수 없는 주문 상태입니다."),
    ORDER_COUPON_ALREADY_APPLIED(HttpStatus.BAD_REQUEST, "ORDER_009", "이미 쿠폰이 적용된 주문입니다."),
    ORDER_COUPON_REQUIRED(HttpStatus.BAD_REQUEST, "ORDER_010", "쿠폰을 적용하려면 발급 쿠폰 ID가 필요합니다."),
    INVALID_ORDER_COUPON_DISCOUNT(HttpStatus.BAD_REQUEST, "ORDER_011", "할인금액은 0 이상이며 쿠폰 적용 대상 금액을 초과할 수 없습니다."),
    INVALID_ORDER_ITEM_PRICE(HttpStatus.BAD_REQUEST, "ORDER_012", "상품 단가는 0 이상이어야 합니다."),
    INVALID_ORDER_ITEM_QUANTITY(HttpStatus.BAD_REQUEST, "ORDER_013", "상품 수량은 1 이상이어야 합니다."),
    ORDER_REQUIRED(HttpStatus.BAD_REQUEST, "ORDER_014", "주문은 필수입니다."),
    INVALID_ORDER_CART_ITEM_IDS(HttpStatus.BAD_REQUEST, "ORDER_015", "장바구니 항목 ID는 양수여야 합니다."),
    DUPLICATE_ORDER_CART_ITEM(HttpStatus.BAD_REQUEST, "ORDER_016", "장바구니 항목을 중복 선택할 수 없습니다."),
    ORDER_ITEM_UNAVAILABLE(HttpStatus.BAD_REQUEST, "ORDER_017", "구매할 수 없는 주문 항목이 있습니다."),

    // 결제
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_001","결제 정보를 찾을 수 없습니다."),
    AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "PAYMENT_002","결제 금액이 일치하지 않습니다."),
    INVALID_PAYMENT_STATUS(HttpStatus.BAD_REQUEST, "PAYMENT_003","처리할 수 없는 결제 상태입니다."),
    PAYMENT_DUPLICATED(HttpStatus.CONFLICT, "PAYMENT_004","이미 결제가 존재하는 주문입니다."),

    // 환불
    REFUND_NOT_FOUND(HttpStatus.NOT_FOUND, "REFUND_001", "환불 정보를 찾을 수 없습니다."),
    REFUND_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "REFUND_002", "환불이 불가능한 결제 건입니다."),
    REFUND_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "REFUND_003", "환불 가능한 상품이 없습니다."),
    INVALID_REFUND_STATUS(HttpStatus.BAD_REQUEST, "REFUND_004", "처리할 수 없는 환불 상태입니다."),

    // 채팅
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_001","채팅방을 찾을 수 없습니다."),
    CHAT_ROOM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CHAT_002","해당 채팅방에 접근할 권한이 없습니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "CHAT_003","허용되지 않은 상태 전이입니다."),
    CLOSED_INQUIRY(HttpStatus.BAD_REQUEST, "CHAT_004","이미 완료된 문의입니다."),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "CHAT_005","유효하지 않은 커서 값입니다."),
    INVALID_INQUIRY_STATUS(HttpStatus.BAD_REQUEST, "CHAT_006","존재하지 않는 문의 상태입니다."),
    WEBSOCKET_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "CHAT_007","웹소켓 인증에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
