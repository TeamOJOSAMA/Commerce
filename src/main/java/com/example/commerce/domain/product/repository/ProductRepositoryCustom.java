package com.example.commerce.domain.product.repository;

import com.example.commerce.domain.product.dto.ProductResponse;
import com.example.commerce.domain.product.dto.SearchProductRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepositoryCustom {
    Page<ProductResponse> searchProductByConditionPage(SearchProductRequest request, Pageable pageable);

}
