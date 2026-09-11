package com.example.commerce.domain.cart.controller;

import com.example.commerce.common.response.ApiResponse;
import com.example.commerce.domain.auth.entity.AuthUser;
import com.example.commerce.domain.cart.dto.AddToCartRequest;
import com.example.commerce.domain.cart.dto.AddToCartResponse;
import com.example.commerce.domain.cart.facade.CartFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<AddToCartResponse>> createCart(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long productId,
            @Valid @RequestBody AddToCartRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(cartFacade.addCartItem(authUser.getUserId(), productId, request)));
    }
}
