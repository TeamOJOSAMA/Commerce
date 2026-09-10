package com.example.commerce.domain.chat.service;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.chat.dto.response.ChatMessageResponse;
import com.example.commerce.domain.chat.dto.response.ChatMessageSliceResponse;
import com.example.commerce.domain.chat.entity.ChatMessage;
import com.example.commerce.domain.chat.entity.ChatRoom;
import com.example.commerce.domain.chat.repository.ChatMessageRepository;
import com.example.commerce.domain.chat.repository.ChatRoomRepository;
import com.example.commerce.domain.user.entity.User;
import com.example.commerce.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatMessageResponse sendMessage(Long chatRoomId, Long senderId, String content) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        User sender = getUser(senderId);

        chatRoom.validateAccessibleBy(sender);
        chatRoom.validateSendable();

        ChatMessage chatMessage =
                chatMessageRepository.save(ChatMessage.ofTalk(chatRoom, sender, content));

        return ChatMessageResponse.from(chatMessage);
    }

    public ChatMessageSliceResponse getMessages(Long chatRoomId,
                                                Long readerId,
                                                Long cursor,
                                                int size,
                                                boolean isAfter) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        User reader = getUser(readerId);

        chatRoom.validateAccessibleBy(reader);
        validateSize(size);

        // hasNext 판정을 위해 한 건 더 조회한다
        Pageable pageable = PageRequest.of(0, size + 1);
        List<ChatMessage> messages = isAfter
                ? chatMessageRepository.findSliceAfter(chatRoomId, requireCursor(cursor), pageable)
                : chatMessageRepository.findSliceBefore(chatRoomId, cursor, pageable);

        boolean hasNext = messages.size() > size;
        if (hasNext) {
            messages = messages.subList(0, size);
        }

        return ChatMessageSliceResponse.of(messages, hasNext);
    }

    private void validateSize(int size) {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    // 복구 조회는 기준점이 반드시 필요하다
    private Long requireCursor(Long cursor) {
        if (cursor == null) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }

        return cursor;
    }

    public ChatRoom getChatRoom(Long chatRoomId) {
        return chatRoomRepository.findByIdWithCustomer(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}