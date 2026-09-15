package com.example.commerce.domain.event.service;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.event.dto.EventResponse;
import com.example.commerce.domain.event.entity.Event;
import com.example.commerce.domain.event.entity.EventStatus;
import com.example.commerce.domain.event.repository.EventCustomRepository;
import com.example.commerce.domain.event.repository.EventRepository;
import com.example.commerce.domain.product.dto.EventInfoRequest;
import com.example.commerce.domain.product.entity.Product;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Getter
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    public Event startEvent(Product product, EventInfoRequest eventInfo){
        LocalDateTime startAt = (eventInfo.startAt() != null) ? eventInfo.startAt() : LocalDateTime.now();
        LocalDateTime endAt = startAt.plusHours(eventInfo.durationHours());

        return eventRepository.save( new Event(
                product,
                eventInfo.discountRate(),
                eventInfo.eventPrice(),
                product.getStock(),
                startAt,
                endAt
        ));

    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getEvents(EventStatus status, Pageable pageable) {
        return eventRepository.searchEvents(status, pageable);
    }

    public EventResponse getEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        return EventResponse.from(event);
    }

    public Optional<Event> findActiveEvent(Long productId) {
        return eventRepository.findByProduct_IdAndStatus(productId, EventStatus.ACTIVE);
    }

    public List<Event> findApplicableEvents(List<Long> productIds) {
        return eventRepository.findApplicableEvents(productIds, EventStatus.ACTIVE, LocalDateTime.now());
    }


}
