package com.example.commerce.domain.chat.dto.response;

import com.example.commerce.domain.chat.entity.ChatMessage;

import java.util.List;

// 커서 기반 페이징 결과
public record ChatMessageSliceResponse(
        List<ChatMessageResponse> messages,
        Long nextCursor,
        boolean hasNext
) {

    public static ChatMessageSliceResponse of(List<ChatMessage> chatMessages, boolean hasNext) {
        List<ChatMessageResponse> messages = chatMessages.stream()
                .map(ChatMessageResponse::from)
                .toList();

        // 다음 페이지가 없으면 커서를 내려줄 필요가 없다
        Long nextCursor = (hasNext && !messages.isEmpty())
                ? messages.get(messages.size() - 1).chatMessageId()
                : null;

        return new ChatMessageSliceResponse(messages, nextCursor, hasNext);
    }
}