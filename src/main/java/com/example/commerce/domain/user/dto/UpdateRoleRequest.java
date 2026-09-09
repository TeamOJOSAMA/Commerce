package com.example.commerce.domain.user.dto;

import com.example.commerce.domain.user.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(
        @NotNull UserRole role
) {
}
