package com.example.commerce.domain.event.entity;

import com.example.commerce.common.entity.BaseEntity;
import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "events",
        indexes = @Index(name = "idx_status_end_at", columnList = "status, end_at")
)
@Getter
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "discount_rate", nullable = false)
    private int discountRate;

    @Column(name = "event_price", nullable = false)
    private Long eventPrice;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "sold_quantity", nullable = false)
    private int soldQuantity;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EventStatus status;

    public Event(Product product, int discountRate, Long eventPrice,
                 int totalQuantity, LocalDateTime startAt, LocalDateTime endAt) {
        validate(product, eventPrice);

        this.product = product;
        this.discountRate = discountRate;
        this.eventPrice = eventPrice;
        this.totalQuantity = totalQuantity;
        this.soldQuantity = 0;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = EventStatus.ACTIVE;
    }

    private void validate(Product product, Long eventPrice) {
        if (eventPrice <= 0 || eventPrice >= product.getPrice()) {
            throw new BusinessException(ErrorCode.INVALID_EVENT_PRICE);
        }
    }

    public void end() {
        this.status = EventStatus.ENDED;
    }
}