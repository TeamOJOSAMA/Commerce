package com.example.commerce.domain.chat.service;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.chat.dto.response.AdminChatRoomResponse;
import com.example.commerce.domain.chat.dto.response.ChatRoomCreateResponse;
import com.example.commerce.domain.chat.dto.response.CustomerChatRoomResponse;
import com.example.commerce.domain.chat.dto.response.InquiryStatusUpdateResponse;
import com.example.commerce.domain.chat.entity.ChatRoom;
import com.example.commerce.domain.chat.entity.InquiryStatus;
import com.example.commerce.domain.chat.repository.ChatRoomRepository;
import com.example.commerce.domain.user.entity.User;
import com.example.commerce.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatRoomCreateResponse createChatRoom(Long customerId, String title) {
        User customer = getUser(customerId);
        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.of(customer, title));

        return ChatRoomCreateResponse.from(chatRoom);
    }

    public Page<CustomerChatRoomResponse> getMyChatRooms(Long customerId,
                                                         InquiryStatus status,
                                                         Pageable pageable) {
        return chatRoomRepository
                .findAllByCustomerIdAndStatus(customerId, status, pageable)
                .map(CustomerChatRoomResponse::from);
    }

    public Page<AdminChatRoomResponse> getAllChatRooms(InquiryStatus status, Pageable pageable) {
        return chatRoomRepository
                .findAllByStatus(status, pageable)
                .map(AdminChatRoomResponse::from);
    }

    @Transactional
    public InquiryStatusUpdateResponse updateStatus(Long chatRoomId,
                                                    Long adminId,
                                                    InquiryStatus targetStatus) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        User admin = getUser(adminId);

        transit(chatRoom, admin, targetStatus);

        return InquiryStatusUpdateResponse.from(chatRoom);
    }

    // 고객이 봇 응대를 종료하고 상담원 연결을 요청
    @Transactional
    public InquiryStatusUpdateResponse escalate(Long chatRoomId, Long customerId) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        User customer = getUser(customerId);

        chatRoom.validateAccessibleBy(customer);
        chatRoom.requestAgent();

        return InquiryStatusUpdateResponse.from(chatRoom);
    }

    // 상태별 전이 메서드가 다르므로 목표 상태에 따라 분기한다
    private void transit(ChatRoom chatRoom, User admin, InquiryStatus targetStatus) {
        switch (targetStatus) {
            case IN_PROGRESS -> chatRoom.assignTo(admin);
            case COMPLETED -> chatRoom.complete();
            // WAITING 으로의 전이는 고객이 escalate API 로 요청한다
            default -> throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }

    private ChatRoom getChatRoom(Long chatRoomId) {
        return chatRoomRepository.findByIdWithCustomer(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}