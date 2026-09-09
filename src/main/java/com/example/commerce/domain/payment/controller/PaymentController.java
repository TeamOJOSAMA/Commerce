package com.example.commerce.domain.payment.controller;

import com.example.commerce.common.response.ApiResponse;
import com.example.commerce.domain.payment.dto.PaymentFailRequest;
import com.example.commerce.domain.payment.dto.PaymentResponse;
import com.example.commerce.domain.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{paymentId}/approve")
    public ApiResponse<PaymentResponse> approvePayment(@PathVariable Long paymentId) {
        return ApiResponse.ok("결제가 승인되었습니다.", paymentService.approvePayment((paymentId)));
    }

    @PostMapping("/{paymentId}/fail")
    public ApiResponse<PaymentResponse> failPayment(@PathVariable Long paymentId, @Valid @RequestBody PaymentFailRequest paymentFailRequest) {
        return ApiResponse.ok("결제가 실패 처리되었습니다.", paymentService.failPayment(paymentId, paymentFailRequest.failReason()));
    }

    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentResponse> getPayment(@PathVariable Long paymentId) {
        return ApiResponse.ok(paymentService.getPaymentDetail(paymentId));
    }
}
