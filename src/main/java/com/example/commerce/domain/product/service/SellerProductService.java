package com.example.commerce.domain.product.service;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.auth.entity.AuthUser;
import com.example.commerce.domain.event.entity.Event;
import com.example.commerce.domain.event.service.EventService;
import com.example.commerce.domain.product.dto.CreateProductRequest;
import com.example.commerce.domain.product.dto.CreateProductResponse;
import com.example.commerce.domain.product.dto.EventInfoRequest;
import com.example.commerce.domain.product.dto.UpdateProductRequest;
import com.example.commerce.domain.product.dto.UpdateProductResponse;
import com.example.commerce.domain.product.entity.Product;
import com.example.commerce.domain.product.entity.ProductStatus;
import com.example.commerce.domain.product.repository.ProductRepository;
import com.example.commerce.domain.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class SellerProductService {

    private final ProductRepository productRepository;
    private final EventService eventService;

    @Transactional
    public CreateProductResponse createProduct(AuthUser authUser, CreateProductRequest request) {
        Product product = new Product(
                authUser.getId(),
                request.name(),
                request.description(),
                request.category(),
                request.price(),
                request.stock()
        );

        Product saved = productRepository.save(product);

        if (request.eventInfo() != null) {
            applyEvent(saved, request.eventInfo());
        }

        return CreateProductResponse.from(saved);
    }

    @Transactional
    public UpdateProductResponse updateProduct(AuthUser authUser, Long productId, UpdateProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        validateOwnership(authUser, product);

        product.update(request.name(), request.description(), request.category(), request.price());

        if (request.status() != null) {
            applyStatus(product, request.status(), request.eventInfo());
        }

        return UpdateProductResponse.from(product);
    }

    private void applyStatus(Product product, ProductStatus status, EventInfoRequest eventInfo) {
        switch (status) {
            case ON_SALE -> product.sale();
            case SOLDOUT -> product.soldout();
            case ON_EVENT -> applyEvent(product, eventInfo);
        }
    }

    // Product는 "이벤트 시작해줘"라고 요청만 함, 실제 Event 생성은 EventService 책임
    private void applyEvent(Product product, EventInfoRequest eventInfo) {
        if (eventInfo == null) {
            throw new BusinessException(ErrorCode.EVENT_INFO_REQUIRED);
        }

        Event event = eventService.startEvent(product.getId(), product.getStock(), eventInfo);

        product.event(eventInfo.eventPrice(), eventInfo.discountRate(), event.getId());
    }

    private void validateOwnership(AuthUser authUser, Product product) {
        if (authUser.getRole() == UserRole.ADMIN) {
            return;
        }
        if (!product.isOwnedBy(authUser.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }
    }
}