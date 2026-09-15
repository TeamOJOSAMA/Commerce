package com.example.commerce.domain.order.controller;

import com.example.commerce.common.response.ApiResponse;
import com.example.commerce.common.response.PageResponse;
import com.example.commerce.domain.auth.entity.AuthUser;
import com.example.commerce.domain.order.dto.CreateOrderRequest;
import com.example.commerce.domain.order.dto.CreateOrderResponse;
import com.example.commerce.domain.order.dto.OrderPreviewResponse;
import com.example.commerce.domain.order.dto.OrderResponse;
import com.example.commerce.domain.order.dto.OrderSummaryResponse;
import com.example.commerce.domain.order.entity.OrderCancelReason;
import com.example.commerce.domain.order.facade.OrderFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    // 쿠폰은 선택이다. 선택하면 서버가 할인액과 결제 예정 금액을 계산해 내려주고, 적용할 수 없으면 쿠폰 오류로 거부한다.
    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<OrderPreviewResponse>> getOrderPreview(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(value = "cartItemIds", required = false) List<Long> cartItemIds,
            @RequestParam(value = "userCouponId", required = false) Long userCouponId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                orderFacade.getOrderPreview(authUser.getUserId(), cartItemIds, userCouponId)
        ));
    }

    /**
     * 멱등성 키로 기존 주문을 그대로 돌려줄 때도 201을 반환한다.
     * 같은 요청의 결과를 다시 주는 것이므로 상태 코드를 나누지 않고,
     * 응답의 status로 그 주문이 아직 결제 가능한지를 구분하게 한다.
     */
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
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> getOrders(
            @AuthenticationPrincipal AuthUser authUser,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(orderFacade.getOrders(authUser.getUserId(), pageable))
        ));
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
            orderFacade.cancelOrder(authUser.getUserId(), orderId, OrderCancelReason.USER_REQUEST);
            return ResponseEntity.ok(ApiResponse.ok("주문이 취소되었습니다."));
    }
}
