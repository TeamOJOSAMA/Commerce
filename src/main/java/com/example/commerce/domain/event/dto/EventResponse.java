package com.example.commerce.domain.event.dto;

import com.example.commerce.domain.event.entity.Event;
import com.example.commerce.domain.event.entity.EventStatus;

import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        Long productId,
        int discountRate,
        Long eventPrice,
        int totalQuantity,
        int soldQuantity,
        LocalDateTime startAt,
        LocalDateTime endAt,
        EventStatus status
) {
    public static EventResponse from(Event event){
        return new EventResponse(
                event.getId(),
                event.getProduct().getId(),
                event.getDiscountRate(),
                event.getEventPrice(),
                event.getTotalQuantity(),
                event.getSoldQuantity(),
                event.getStartAt(),
                event.getEndAt(),
                event.getStatus()
        );
    }


}
