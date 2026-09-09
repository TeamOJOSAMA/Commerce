package com.example.commerce.common.response;

import com.example.commerce.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class ApiErrorResponse {

    private final String code;
    private final String message;

    private ApiErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static ApiErrorResponse of(ErrorCode errorCode) {
        return new ApiErrorResponse(errorCode.getCode(), errorCode.getMessage());
    }

    // BusinessException처럼 메시지를 상황에 맞게 덮어쓰고 싶을 때
    public static ApiErrorResponse of(ErrorCode errorCode, String message) {
        return new ApiErrorResponse(errorCode.getCode(), message);
    }
}