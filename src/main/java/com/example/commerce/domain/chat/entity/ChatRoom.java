package com.example.commerce.domain.chat.entity;

import com.example.commerce.common.entity.BaseEntity;
import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
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
        name = "chat_rooms",
        indexes = {
                @Index(name = "idx_chat_rooms_status_created", columnList = "inquiry_status, created_at"),
                @Index(name = "idx_chat_rooms_user_created", columnList = "user_id, created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {

    private static final int TITLE_MAX_LENGTH = 30;
    private static final int STATUS_MAX_LENGTH = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long id;

    // 문의를 등록한 고객
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User customer;

    // 담당 관리자. 상담원 배정 전에는 null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(name = "title", nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "inquiry_status", nullable = false, length = STATUS_MAX_LENGTH)
    private InquiryStatus inquiryStatus;

    private ChatRoom(User customer, String title) {
        this.customer = customer;
        this.title = title;
        this.inquiryStatus = InquiryStatus.BOT_HANDLING;
    }

    public static ChatRoom of(User customer, String title) {
        return new ChatRoom(customer, title);
    }

    /* ===================== 상태 전이 ===================== */

    // 봇 응대 -> 상담원 연결 대기
    public void requestAgent() {
        inquiryStatus.validateTransitionTo(InquiryStatus.WAITING);
        this.inquiryStatus = InquiryStatus.WAITING;
    }

    // 관리자가 문의를 배정받아 처리를 시작
    public void assignTo(User admin) {
        inquiryStatus.validateTransitionTo(InquiryStatus.IN_PROGRESS);
        this.assignee = admin;
        this.inquiryStatus = InquiryStatus.IN_PROGRESS;
    }

    public void complete() {
        inquiryStatus.validateTransitionTo(InquiryStatus.COMPLETED);
        this.inquiryStatus = InquiryStatus.COMPLETED;
    }

    /* ===================== 검증 ===================== */

    public void validateSendable() {
        if (inquiryStatus.isClosed()) {
            throw new BusinessException(ErrorCode.CLOSED_INQUIRY);
        }
    }

    public void validateAccessibleBy(User user) {
        if (!isAccessibleBy(user)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }

    // 관리자는 모든 문의에, 고객은 본인이 생성한 문의에만 접근 가능
    public boolean isAccessibleBy(User user) {
        return user.isAdmin() || isOwnedBy(user);
    }

    public boolean isOwnedBy(User user) {
        return customer.getId().equals(user.getId());
    }

    public boolean isBotHandling() {
        return inquiryStatus.isBotHandling();
    }
}