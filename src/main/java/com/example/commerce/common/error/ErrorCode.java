package com.example.commerce.common.error;

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

    // 상품
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_001","상품을 찾을 수 없습니다."),
    OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "PRODUCT_002","상품의 재고가 부족합니다."),

    // 장바구니
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "CART_001","장바구니 상품을 찾을 수 없습니다."),

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

    // 결제
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_001","결제 정보를 찾을 수 없습니다."),
    AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "PAYMENT_002","결제 금액이 일치하지 않습니다."),
    INVALID_PAYMENT_STATUS(HttpStatus.BAD_REQUEST, "PAYMENT_003","처리할 수 없는 결제 상태입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
