package com.example.commerce.domain.cart.controller;

import com.example.commerce.common.response.ApiResponse;
import com.example.commerce.domain.auth.entity.AuthUser;
import com.example.commerce.domain.cart.dto.AddToCartRequest;
import com.example.commerce.domain.cart.dto.AddToCartResponse;
import com.example.commerce.domain.cart.dto.GetCartResponse;
import com.example.commerce.domain.cart.dto.UpdateQuantityRequest;
import com.example.commerce.domain.cart.dto.UpdateQuantityResponse;
import com.example.commerce.domain.cart.facade.CartFacade;
import com.example.commerce.domain.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/carts/items")
@RequiredArgsConstructor
public class CartController {

    private final CartFacade cartFacade;
    private final CartService cartService;

    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<AddToCartResponse>> createCart(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long productId,
            @Valid @RequestBody AddToCartRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse
                        .ok(
                                "해당 상품을 장바구니에 담았습니다.",
                                cartFacade.addCartItem(authUser.getUserId(), productId, request)
                        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<GetCartResponse>> getCart(@AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity
                .ok(ApiResponse
                        .ok(
                                "장바구니에 있는 상품 목록을 열었습니다.",
                                cartService.getCart(authUser.getUserId())
                        ));
    }

    @PatchMapping("/{cartItemId}")
    public ResponseEntity<ApiResponse<UpdateQuantityResponse>> updateQuantity(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateQuantityRequest request
    ) {
        return ResponseEntity
                .ok(ApiResponse
                        .ok(
                                "해당 상품의 수량을 변경했습니다.",
                                cartService.updateQuantity(authUser.getUserId(), cartItemId, request)
                        ));
    }

    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> deleteCartItem(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long cartItemId
    ) {
        cartService.deleteCartItem(authUser.getUserId(), cartItemId);

        return ResponseEntity.noContent().build(); // 204

        // 참고로 204 No Content는 응답 본문을 포함하지 않으므로 상태 코드만 반환
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteAllCartItem(@AuthenticationPrincipal AuthUser authUser) {

        cartService.deleteAllCartItem(authUser.getUserId());

        return ResponseEntity.noContent().build(); // 204
    }
}
