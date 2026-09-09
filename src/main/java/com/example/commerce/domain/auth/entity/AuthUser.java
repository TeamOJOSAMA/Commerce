package com.example.commerce.domain.auth.entity;

import com.example.commerce.domain.user.entity.UserRole;
import lombok.Getter;

@Getter
public class AuthUser {

    private final Long userId;
    private final String email;
    private final UserRole role;

    public AuthUser(Long userId, String email, UserRole role) {
        this.userId = userId;
        this.email = email;
        this.role = role;
    }
}