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

    // 상품
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_001","상품을 찾을 수 없습니다."),
    OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "PRODUCT_002","상품의 재고가 부족합니다."),
    INVALID_PRODUCT_STATUS(HttpStatus.NOT_ACCEPTABLE, "PRODUCT_003","유호하지 않은 상태입니다"),
    NOT_SUFFICIENT_AMOUNT(HttpStatus.BAD_REQUEST,"PRODUCT_4","최소 가격은 0 이상이여야 합니다"),
    PRICE_ERROR(HttpStatus.BAD_REQUEST,"PRODUCT_5","최소 가격이 최대 가격보다 클수 없습니다"),

    // 이벤트
    INVALID_EVENT_PRICE(HttpStatus.BAD_REQUEST,"EVENT_001", "이벤트중인 상품가격이 필요합니다"),
    EVENT_INFO_REQUIRED(HttpStatus.BAD_REQUEST,"EVENT_002", "이벤트 세부 내용이 없습니다"),
    // 장바구니
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "CART_001","장바구니 상품을 찾을 수 없습니다."),

    // 주문
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_001","주문을 찾을 수 없습니다."),
    ALREADY_CANCELED(HttpStatus.BAD_REQUEST, "ORDER_002","이미 취소된 주문입니다."),

    // 결제
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_001","결제 정보를 찾을 수 없습니다."),
    AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "PAYMENT_002","결제 금액이 일치하지 않습니다."),
    INVALID_PAYMENT_STATUS(HttpStatus.BAD_REQUEST, "PAYMENT_003","처리할 수 없는 결제 상태입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
