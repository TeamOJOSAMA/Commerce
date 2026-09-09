package com.example.commerce.domain.chat.entity;

import com.example.commerce.common.entity.BaseEntity;
import com.example.commerce.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "chat_messages",
        indexes = @Index(name = "idx_chat_messages_room_id", columnList = "chat_room_id, chat_message_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseEntity {

    private static final int TYPE_MAX_LENGTH = 30;
    private static final int CONTENT_MAX_LENGTH = 1000;
    private static final String ENTER_MESSAGE_FORMAT = "%s님이 입장했습니다.";
    private static final String LEAVE_MESSAGE_FORMAT = "%s님이 퇴장했습니다.";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_message_id")
    private Long id;

    // 메시지 -> 채팅방 단방향 참조 (양방향 X)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = TYPE_MAX_LENGTH)
    private MessageType messageType;

    @Column(name = "content", nullable = false, length = CONTENT_MAX_LENGTH)
    private String content;

    private ChatMessage(ChatRoom chatRoom, User sender, MessageType messageType, String content) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.messageType = messageType;
        this.content = content;
    }

    public static ChatMessage ofTalk(ChatRoom chatRoom, User sender, String content) {
        return new ChatMessage(chatRoom, sender, MessageType.TALK, content);
    }

    public static ChatMessage ofBot(ChatRoom chatRoom, User bot, String content) {
        return new ChatMessage(chatRoom, bot, MessageType.BOT, content);
    }

    public static ChatMessage ofEnter(ChatRoom chatRoom, User sender) {
        return new ChatMessage(chatRoom, sender, MessageType.ENTER,
                ENTER_MESSAGE_FORMAT.formatted(sender.getName()));
    }

    public static ChatMessage ofLeave(ChatRoom chatRoom, User sender) {
        return new ChatMessage(chatRoom, sender, MessageType.LEAVE,
                LEAVE_MESSAGE_FORMAT.formatted(sender.getName()));
    }
}