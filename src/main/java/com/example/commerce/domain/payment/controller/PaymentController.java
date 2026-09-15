package com.example.commerce.domain.payment.controller;

import com.example.commerce.common.response.ApiResponse;
import com.example.commerce.domain.auth.entity.AuthUser;
import com.example.commerce.domain.payment.dto.PaymentFailRequest;
import com.example.commerce.domain.payment.dto.PaymentResponse;
import com.example.commerce.domain.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{paymentId}/approve")
    public ResponseEntity<ApiResponse<PaymentResponse>> approvePayment(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long paymentId
    ) {
        return ResponseEntity.ok(ApiResponse.ok("결제가 승인되었습니다.", paymentService.approvePayment(authUser.getUserId(), paymentId)));
    }

    @PostMapping("/{paymentId}/fail")
    public ResponseEntity<ApiResponse<PaymentResponse>> failPayment(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentFailRequest paymentFailRequest
    ) {
        return ResponseEntity.ok(ApiResponse.ok("결제가 실패 처리되었습니다.",
                paymentService.failPayment(authUser.getUserId(), paymentId, paymentFailRequest.failReason())));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long paymentId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getPaymentDetail(authUser.getUserId(), paymentId)));
    }
}
