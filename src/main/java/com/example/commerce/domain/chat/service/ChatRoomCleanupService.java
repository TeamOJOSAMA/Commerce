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

// 봇 응대 중인 채팅방이 일정 시간 방치되면 자동으로 종료한다.
// closeIdleChatRooms() 자체에 @Transactional을 붙여, 스케줄러 호출이 프록시를 거쳐
// 트랜잭션을 연 상태에서 내부적으로 closeAndNotify를 this::로 호출하게 한다.
// (self-invocation이어도 이미 열린 트랜잭션에 참여하므로 영속성 컨텍스트가 유지된다.)
// 단, 유휴 방 전체가 하나의 트랜잭션으로 묶이므로 한 방 처리 중 예외가 나면
// 그 배치의 나머지 방 처리도 함께 롤백된다.
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

    @Transactional
    @Scheduled(fixedDelayString = "${chat.cleanup-interval-ms}")
    public void closeIdleChatRooms() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(idleTimeoutMinutes);
        List<Long> idleChatRoomIds = chatRoomRepository.findIdleChatRoomIds(threshold);

        idleChatRoomIds.forEach(this::closeAndNotify);
    }

    private void closeAndNotify(Long chatRoomId) {
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
