package com.example.commerce.domain.auth.service;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.common.jwt.JwtProvider;
import com.example.commerce.domain.auth.dto.LoginRequest;
import com.example.commerce.domain.auth.dto.LoginResponse;
import com.example.commerce.domain.auth.dto.SignupRequest;
import com.example.commerce.domain.user.entity.User;
import com.example.commerce.domain.user.entity.UserRole;
import com.example.commerce.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String NAME = "tester";
    private static final String EMAIL = "test@test.com";
    private static final String RAW_PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encoded-password";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;

    // -------- signup --------

    @Test
    @DisplayName("회원가입 성공 - USER 권한으로 저장된다")
    void signup_success() {
        SignupRequest request = new SignupRequest(NAME, EMAIL, RAW_PASSWORD);

        given(userRepository.existsByEmail(EMAIL)).willReturn(false);
        given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(ENCODED_PASSWORD);
        given(userRepository.saveAndFlush(any(User.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        authService.signup(request);

        then(userRepository).should().saveAndFlush(any(User.class));
    }

    @Test
    @DisplayName("이메일이 이미 존재하면 DUPLICATE_EMAIL, 저장하지 않는다")
    void signup_duplicateEmail() {
        SignupRequest request = new SignupRequest(NAME, EMAIL, RAW_PASSWORD);

        given(userRepository.existsByEmail(EMAIL)).willReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);

        then(userRepository).should(never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("동시 요청으로 유니크 제약 위반이 나면 DUPLICATE_EMAIL로 변환한다 (2차 방어)")
    void signup_duplicateEmail_raceCondition() {
        SignupRequest request = new SignupRequest(NAME, EMAIL, RAW_PASSWORD);

        given(userRepository.existsByEmail(EMAIL)).willReturn(false);
        given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(ENCODED_PASSWORD);
        given(userRepository.saveAndFlush(any(User.class)))
                .willThrow(new DataIntegrityViolationException("uk_users_email"));

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }

    // -------- login --------

    @Test
    @DisplayName("로그인 성공 - 토큰을 발급한다")
    void login_success() {
        LoginRequest request = new LoginRequest(EMAIL, RAW_PASSWORD);
        User user = User.of(NAME, EMAIL, ENCODED_PASSWORD, UserRole.USER);

        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
        given(jwtProvider.createToken(user.getId(), EMAIL, UserRole.USER)).willReturn("Bearer token");

        LoginResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("Bearer token");
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인하면 LOGIN_FAILED")
    void login_userNotFound() {
        LoginRequest request = new LoginRequest(EMAIL, RAW_PASSWORD);

        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.LOGIN_FAILED);
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 LOGIN_FAILED")
    void login_wrongPassword() {
        LoginRequest request = new LoginRequest(EMAIL, "wrong-password");
        User user = User.of(NAME, EMAIL, ENCODED_PASSWORD, UserRole.USER);

        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong-password", ENCODED_PASSWORD)).willReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.LOGIN_FAILED);
    }
}
