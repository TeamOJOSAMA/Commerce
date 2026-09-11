package com.example.commerce.domain.chat.controller;

import com.example.commerce.common.response.ApiResponse;
import com.example.commerce.common.response.PageResponse;
import com.example.commerce.domain.auth.entity.AuthUser;
import com.example.commerce.domain.chat.dto.request.ChatRoomCreateRequest;
import com.example.commerce.domain.chat.dto.request.InquiryStatusUpdateRequest;
import com.example.commerce.domain.chat.dto.response.AdminChatRoomResponse;
import com.example.commerce.domain.chat.dto.response.ChatRoomCreateResponse;
import com.example.commerce.domain.chat.dto.response.CustomerChatRoomResponse;
import com.example.commerce.domain.chat.dto.response.InquiryStatusUpdateResponse;
import com.example.commerce.domain.chat.entity.InquiryStatus;
import com.example.commerce.domain.chat.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// context-path 를 사용하지 않으므로 경로에 /api 를 포함한다
@RestController
@RequestMapping("/api/v1/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @PostMapping
    public ResponseEntity<ApiResponse<ChatRoomCreateResponse>> createChatRoom(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody ChatRoomCreateRequest request) {

        ChatRoomCreateResponse response =
                chatRoomService.createChatRoom(authUser.getUserId(), request.title());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("CS 문의가 접수되었습니다.", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<CustomerChatRoomResponse>>> getMyChatRooms(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) InquiryStatus status,
            @PageableDefault(size = 10) Pageable pageable) {

        PageResponse<CustomerChatRoomResponse> response = PageResponse.from(
                chatRoomService.getMyChatRooms(authUser.getUserId(), status, pageable));

        return ResponseEntity.ok(ApiResponse.ok("내 문의 목록 조회에 성공했습니다.", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AdminChatRoomResponse>>> getAllChatRooms(
            @RequestParam(required = false) InquiryStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        PageResponse<AdminChatRoomResponse> response = PageResponse.from(
                chatRoomService.getAllChatRooms(status, pageable));

        return ResponseEntity.ok(ApiResponse.ok("CS 문의 목록 조회에 성공했습니다.", response));
    }

    @PatchMapping("/{chatRoomId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InquiryStatusUpdateResponse>> updateStatus(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long chatRoomId,
            @Valid @RequestBody InquiryStatusUpdateRequest request) {

        InquiryStatusUpdateResponse response = chatRoomService.updateStatus(
                chatRoomId, authUser.getUserId(), request.inquiryStatus());

        return ResponseEntity.ok(ApiResponse.ok("문의 상태가 변경되었습니다.", response));
    }

    @PostMapping("/{chatRoomId}/escalate")
    public ResponseEntity<ApiResponse<InquiryStatusUpdateResponse>> escalate(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long chatRoomId) {

        InquiryStatusUpdateResponse response =
                chatRoomService.escalate(chatRoomId, authUser.getUserId());

        return ResponseEntity.ok(ApiResponse.ok("상담원 연결을 요청했습니다.", response));
    }
}