package com.example.commerce.domain.chat.repository;

import com.example.commerce.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 과거 메시지 조회. cursor 미지정 시 최신부터
    @Query("""
            SELECT m FROM ChatMessage m
            JOIN FETCH m.sender
            WHERE m.chatRoom.id = :chatRoomId
              AND (:cursor IS NULL OR m.id < :cursor)
            ORDER BY m.id DESC
            """)
    List<ChatMessage> findSliceBefore(@Param("chatRoomId") Long chatRoomId,
                                      @Param("cursor") Long cursor,
                                      Pageable pageable);

    // 재연결 시 누락 메시지 복구
    @Query("""
            SELECT m FROM ChatMessage m
            JOIN FETCH m.sender
            WHERE m.chatRoom.id = :chatRoomId
              AND m.id > :cursor
            ORDER BY m.id ASC
            """)
    List<ChatMessage> findSliceAfter(@Param("chatRoomId") Long chatRoomId,
                                     @Param("cursor") Long cursor,
                                     Pageable pageable);
}