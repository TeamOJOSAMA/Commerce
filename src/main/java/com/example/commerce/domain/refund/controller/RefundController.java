package com.example.commerce.domain.refund.controller;

import com.example.commerce.common.response.ApiResponse;
import com.example.commerce.domain.refund.dto.RefundRequest;
import com.example.commerce.domain.refund.dto.RefundResponse;
import com.example.commerce.domain.refund.repository.RefundRepository;
import com.example.commerce.domain.refund.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping
    public ApiResponse<RefundResponse> createRefund(@Valid @RequestBody RefundRequest refundRequest) {
        return ApiResponse.ok("환불이 접수되었습니다.", refundService.createRefund(refundRequest));
    }
}
