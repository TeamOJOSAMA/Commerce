package com.example.commerce.domain.user.controller;

import com.example.commerce.common.response.ApiResponse;
import com.example.commerce.domain.user.dto.UpdateRoleRequest;
import com.example.commerce.domain.user.dto.UserResponse;
import com.example.commerce.domain.user.entity.UserRole;
import com.example.commerce.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(
            @RequestParam(required = false) UserRole role,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok("유저 목록 조회에 성공했습니다.", userService.getUsers(role, pageable)));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<Void>> updateRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        userService.updateRole(userId, request.role());

        return ResponseEntity.ok(ApiResponse.ok("권한 변경에 성공했습니다."));
    }
}