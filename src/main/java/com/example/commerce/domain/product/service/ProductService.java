package com.example.commerce.domain.product.service;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.event.entity.Event;
import com.example.commerce.domain.event.service.EventService;
import com.example.commerce.domain.product.dto.ProductResponse;
import com.example.commerce.domain.product.dto.SearchProductRequest;
import com.example.commerce.domain.product.entity.Product;
import com.example.commerce.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final EventService eventService;

    public Page<ProductResponse> searchProduct(SearchProductRequest request, Pageable pageable) {
        validateSearchCondition(request);

        return productRepository.searchProductByConditionPage(request, pageable);
    }

    @Transactional
    public ProductResponse getProductDetail(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.increaseViewCount();

        Event activeEvent = eventService.findActiveEvent(id).orElse(null);

        return ProductResponse.of(product, activeEvent);
    }

    public Page<ProductResponse> getPopularProducts(Pageable pageable) {
        return productRepository.findPopularProducts(pageable);
    }

    private void validateSearchCondition(SearchProductRequest request) {
        if (request.minPrice() != null && request.minPrice() < 0) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        if (request.maxPrice() != null && request.maxPrice() < 0) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        if (request.minPrice() != null && request.maxPrice() != null
                && request.minPrice() > request.maxPrice()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
    }

    public List<Product> findAllForUpdate(List<Long> productIds) {
        return productRepository.findAllByIdsForUpdate(productIds);
    }
}