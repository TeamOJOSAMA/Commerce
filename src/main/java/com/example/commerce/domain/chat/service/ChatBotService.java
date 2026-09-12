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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatBotService {

    private static final String ESCALATE_KEYWORD = "상담사 연결";
    private static final String CLOSE_KEYWORD = "종료";
    private static final String CONTINUE_KEYWORD = "계속";

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    @Value("${chat.bot-user-id}")
    private Long botUserId;

    // 봇 응대 상태에서만 자동응답한다. 상담원 연결 이후에는 사람이 답한다
    public boolean isBotTurn(ChatRoom chatRoom) {
        return chatRoom.isBotHandling();
    }

    public boolean isEscalateRequest(String message) {
        return message.contains(ESCALATE_KEYWORD);
    }

    public boolean isCloseRequest(String message) {
        return message.contains(CLOSE_KEYWORD);
    }

    public boolean isContinueRequest(String message) {
        return message.contains(CONTINUE_KEYWORD);
    }

    @Transactional
    public ChatMessageResponse reply(Long chatRoomId, String customerMessage) {
        return saveBotMessage(chatRoomId, BotScenario.replyTo(customerMessage));
    }

    // 고객이 상담사 연결을 요청하면 안내 메시지를 남기고 상태를 전이한다
    @Transactional
    public ChatMessageResponse escalate(Long chatRoomId) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        chatRoom.requestAgent();

        return saveBotMessage(chatRoomId, BotScenario.ESCALATE_REPLY);
    }

    @Transactional
    public ChatMessageResponse close(Long chatRoomId) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        chatRoom.complete();

        return saveBotMessage(chatRoomId, BotScenario.CLOSED_REPLY);
    }

    @Transactional
    public ChatMessageResponse resume(Long chatRoomId) {
        return saveBotMessage(chatRoomId, BotScenario.CONTINUE_REPLY);
    }

    private ChatMessageResponse saveBotMessage(Long chatRoomId, String content) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        User bot = getBotUser();

        ChatMessage botMessage =
                chatMessageRepository.save(ChatMessage.ofBot(chatRoom, bot, content));

        return ChatMessageResponse.from(botMessage);
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