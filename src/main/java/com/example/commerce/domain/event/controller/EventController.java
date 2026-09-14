package com.example.commerce.domain.event.controller;

import com.example.commerce.common.response.ApiResponse;
import com.example.commerce.domain.event.dto.EventResponse;
import com.example.commerce.domain.event.entity.EventStatus;
import com.example.commerce.domain.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService eventService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EventResponse>>> getEvents(
            @RequestParam(required = false) EventStatus status,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getEvents(status, pageable)));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getEvent(eventId)));
    }

}
