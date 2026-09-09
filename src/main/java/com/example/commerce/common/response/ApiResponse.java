package com.example.commerce.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private static final String SUCCESS_CODE = "SUCCESS";

    private final String code;
    private final String message;
    private final T data;

    public ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(SUCCESS_CODE, null, data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(SUCCESS_CODE, message, data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(SUCCESS_CODE, null, null);
    }
}
