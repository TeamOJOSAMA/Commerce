package com.example.commerce.domain.refund.controller;

import com.example.commerce.common.response.ApiResponse;
import com.example.commerce.domain.auth.entity.AuthUser;
import com.example.commerce.domain.refund.dto.RefundRequest;
import com.example.commerce.domain.refund.dto.RefundResponse;
import com.example.commerce.domain.refund.repository.RefundRepository;
import com.example.commerce.domain.refund.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping
    public ApiResponse<RefundResponse> createRefund(@AuthenticationPrincipal AuthUser authUser,
                                                    @Valid @RequestBody RefundRequest refundRequest) {
        return ApiResponse.ok("환불이 접수되었습니다.", refundService.createRefund(authUser.getUserId(),refundRequest));
    }

    @PostMapping("/{refundId}/complete")
    public ApiResponse<RefundResponse> completeRefund(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long refundId) {
        return ApiResponse.ok("환불이 완료되었습니다.", refundService.completeRefund(authUser.getUserId(),refundId));
    }
}
