package com.example.commerce.domain.user.controller;

import com.example.commerce.common.response.ApiResponse;
import com.example.commerce.domain.auth.entity.AuthUser;
import com.example.commerce.domain.user.dto.UserResponse;
import com.example.commerce.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(@AuthenticationPrincipal AuthUser authUser) {
        UserResponse response = userService.getMe(authUser.getUserId());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
