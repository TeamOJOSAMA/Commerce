package com.example.commerce.domain.order.controller;

import com.example.commerce.common.response.ApiResponse;
import com.example.commerce.domain.auth.entity.AuthUser;
import com.example.commerce.domain.order.dto.CreateOrderRequest;
import com.example.commerce.domain.order.dto.CreateOrderResponse;
import com.example.commerce.domain.order.dto.OrderPreviewResponse;
import com.example.commerce.domain.order.dto.OrderResponse;
import com.example.commerce.domain.order.dto.OrderSummaryResponse;
import com.example.commerce.domain.order.facade.OrderFacade;
import com.example.commerce.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderFacade orderFacade;
    private final OrderService orderService;

    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<OrderPreviewResponse>> getOrderPreview(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(value = "cartItemIds", required = false) List<Long> cartItemIds
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                orderFacade.getOrderPreview(authUser.getUserId(), cartItemIds)
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrder(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                orderFacade.createOrder(authUser.getUserId(), request)
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderSummaryResponse>>> getOrders(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrders(authUser.getUserId())));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable("orderId") Long orderId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(orderFacade.getOrder(authUser.getUserId(), orderId)));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable("orderId") Long orderId
    ) {
        orderFacade.cancelOrder(authUser.getUserId(), orderId);
        return ResponseEntity.ok(ApiResponse.ok("주문이 취소되었습니다."));
    }
}
