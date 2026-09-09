package com.example.commerce.domain.cart.entity;

import com.example.commerce.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO: 회원 도메인 연동 후 User 외래키로 바꿀 예정
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    public Cart(Long userId) {
        this.userId = userId;
    }
}
