package com.example.commerce.domain.chat.service;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.chat.dto.response.ChatMessageResponse;
import com.example.commerce.domain.chat.entity.BotScenario;
import com.example.commerce.domain.chat.entity.ChatMessage;
import com.example.commerce.domain.chat.entity.ChatRoom;
import com.example.commerce.domain.chat.repository.ChatMessageRepository;
import com.example.commerce.domain.chat.repository.ChatRoomRepository;
import com.example.commerce.domain.user.entity.User;
import com.example.commerce.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// 봇 응대 중인 채팅방이 일정 시간 방치되면 자동으로 종료함
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomCleanupService {

    private static final String SUBSCRIBE_DESTINATION = "/sub/chat-rooms/";
    private static final String STATUS_DESTINATION_SUFFIX = "/status";
    private static final String COMPLETED_STATUS = "COMPLETED";

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${chat.bot-user-id}")
    private Long botUserId;

    @Value("${chat.idle-timeout-minutes}")
    private long idleTimeoutMinutes;

    @Scheduled(fixedDelayString = "${chat.cleanup-interval-ms}")
    public void closeIdleChatRooms() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(idleTimeoutMinutes);
        List<Long> idleChatRoomIds = chatMessageRepository.findIdleChatRoomIds(threshold);

        idleChatRoomIds.forEach(this::closeAndNotify);
    }

    @Transactional
    public void closeAndNotify(Long chatRoomId) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        User bot = getBotUser();

        chatRoom.complete();

        ChatMessage botMessage = chatMessageRepository.save(
                ChatMessage.ofBot(chatRoom, bot, BotScenario.AUTO_CLOSED_REPLY));

        messagingTemplate.convertAndSend(
                SUBSCRIBE_DESTINATION + chatRoomId, ChatMessageResponse.from(botMessage));
        messagingTemplate.convertAndSend(
                SUBSCRIBE_DESTINATION + chatRoomId + STATUS_DESTINATION_SUFFIX, COMPLETED_STATUS);

        log.info("방치된 채팅방 자동 종료: roomId={}", chatRoomId);
    }

    private ChatRoom getChatRoom(Long chatRoomId) {
        return chatRoomRepository.findByIdWithCustomer(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private User getBotUser() {
        return userRepository.findById(botUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}