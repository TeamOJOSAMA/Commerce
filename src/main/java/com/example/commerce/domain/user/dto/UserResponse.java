package com.example.commerce.domain.user.dto;

import com.example.commerce.domain.user.entity.User;
import com.example.commerce.domain.user.entity.UserRole;

public record UserResponse(
        Long id,
        String name,
        String email,
        UserRole role
) {
    public static UserResponse from(User user) {

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole());
    }
}