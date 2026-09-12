package com.example.commerce.domain.product.controller;

import com.example.commerce.common.response.ApiResponse;
import com.example.commerce.domain.auth.entity.AuthUser;
import com.example.commerce.domain.product.dto.CreateProductRequest;
import com.example.commerce.domain.product.dto.CreateProductResponse;
import com.example.commerce.domain.product.dto.UpdateProductRequest;
import com.example.commerce.domain.product.dto.UpdateProductResponse;
import com.example.commerce.domain.product.service.SellerProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/seller/products")
public class SellerProductController {

    private final SellerProductService sellerProductService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateProductResponse>> createProduct(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateProductRequest request
    ) {
        CreateProductResponse response = sellerProductService.createProduct(authUser, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<ApiResponse<UpdateProductResponse>> updateProduct(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        UpdateProductResponse response = sellerProductService.updateProduct(authUser, productId, request);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}