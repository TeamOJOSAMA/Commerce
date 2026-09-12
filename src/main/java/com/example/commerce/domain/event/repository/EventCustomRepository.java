package com.example.commerce.domain.event.repository;

import com.example.commerce.domain.event.dto.EventResponse;
import com.example.commerce.domain.event.entity.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventCustomRepository {
    Page<EventResponse> searchEvents(EventStatus  eventStatus, Pageable pageable);
}
