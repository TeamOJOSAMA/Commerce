package com.example.commerce.domain.product.controller;

import com.example.commerce.common.response.ApiResponse;
import com.example.commerce.domain.product.dto.ProductResponse;
import com.example.commerce.domain.product.dto.SearchProductRequest;
import com.example.commerce.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            SearchProductRequest request,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(productService.searchProduct(request, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getProductDetail(id)));
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getPopularProducts(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getPopularProducts(pageable)));
    }
}