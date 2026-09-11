package com.example.commerce.domain.chat.repository;

import com.example.commerce.domain.chat.entity.ChatRoom;
import com.example.commerce.domain.chat.entity.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 관리자 전체 목록 조회. status 가 null 이면 전체 조회
    @Query("""
            SELECT r FROM ChatRoom r
            JOIN FETCH r.customer
            LEFT JOIN FETCH r.assignee
            WHERE (:status IS NULL OR r.inquiryStatus = :status)
            ORDER BY r.createdAt DESC
            """)
    Page<ChatRoom> findAllByStatus(@Param("status") InquiryStatus status, Pageable pageable);

    // 고객 본인 문의 목록 조회
    @Query("""
            SELECT r FROM ChatRoom r
            LEFT JOIN FETCH r.assignee
            WHERE r.customer.id = :customerId
              AND (:status IS NULL OR r.inquiryStatus = :status)
            ORDER BY r.createdAt DESC
            """)
    Page<ChatRoom> findAllByCustomerIdAndStatus(@Param("customerId") Long customerId,
                                                @Param("status") InquiryStatus status,
                                                Pageable pageable);

    // 단건 조회. 접근 권한 검증에 customer 가 필요해 함께 로딩
    @Query("""
            SELECT r FROM ChatRoom r
            JOIN FETCH r.customer
            WHERE r.id = :chatRoomId
            """)
    Optional<ChatRoom> findByIdWithCustomer(@Param("chatRoomId") Long chatRoomId);

    // 구독 권한 검증용. 고객 본인이거나 관리자면 접근 가능
    @Query("""
            SELECT COUNT(r) > 0 FROM ChatRoom r
            WHERE r.id = :chatRoomId
              AND (:isAdmin = true OR r.customer.id = :userId)
            """)
    boolean existsAccessibleBy(@Param("chatRoomId") Long chatRoomId,
                               @Param("userId") Long userId,
                               @Param("isAdmin") boolean isAdmin);}