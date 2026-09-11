package com.example.commerce.domain.auth.controller;

import com.example.commerce.common.response.ApiResponse;
import com.example.commerce.domain.auth.service.AuthService;
import com.example.commerce.domain.auth.dto.LoginRequest;
import com.example.commerce.domain.auth.dto.LoginResponse;
import com.example.commerce.domain.auth.dto.SignupRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;



    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("회원가입에 성공했습니다."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(ApiResponse.ok("로그인에 성공했습니다.", response));
    }
}
