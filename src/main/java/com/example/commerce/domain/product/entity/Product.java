package com.example.commerce.domain.product.entity;

import com.example.commerce.common.entity.BaseEntity;
import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "products")
@Getter
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sellerId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private ProductCategory category;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private int stock;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    private Long viewCount = 0L;

    public Product(Long sellerId, String name, String description,
                   ProductCategory category, Long price, int stock) {
        this.sellerId = sellerId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.status = (stock > 0) ? ProductStatus.ON_SALE : ProductStatus.SOLDOUT;
    }

    public void update(String name, String description, ProductCategory category, Long price) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (description != null && !description.isBlank()) {
            this.description = description;
        }
        if (category != null) {
            this.category = category;
        }
        if (price != null && price > 0) {
            this.price = price;
        }
    }

    public boolean isOwnedBy(Long sellerId) {
        return this.sellerId.equals(sellerId);
    }

    private void setStatus(ProductStatus nextStatus) {
        if (!this.status.canTransitTo(nextStatus)) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_STATUS);
        }
        this.status = nextStatus;
    }

    public void sale() {
        setStatus(ProductStatus.ON_SALE);
    }

    public void soldout() {
        setStatus(ProductStatus.SOLDOUT);
    }

    public void event() {
        setStatus(ProductStatus.ON_EVENT);
    }

    public void decreaseStock(int quantity) {
        if (quantity <= 0 || this.stock < quantity) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }
        this.stock -= quantity;
    }

    public void restoreStock(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.NOT_SUFFICIENT_AMOUNT);
        }
        this.stock += quantity;
        if (this.status == ProductStatus.SOLDOUT && this.stock > 0) {
            this.status = ProductStatus.ON_SALE;
        }
    }

    public void increaseViewCount() {
        this.viewCount++;
    }
}