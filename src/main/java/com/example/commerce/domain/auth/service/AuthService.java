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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.of(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password()),
                UserRole.USER
        );

        // 이메일 중복 검사 시 저장까지의 시간차가 있다.
        // 이 시간차로 인해 동시성 문제가 발생 할 수 있으니 방지하기
        // 흐름: 평소에는 existsByEmail이 DUPLICATE_EMAIL을 던지고
        //      드물게 요청이 동시에 체크를 통과하면 DataIntegrityViolationException이 잡아서 던진다.
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        String token = jwtProvider.createToken(user.getId(), user.getEmail(), user.getRole());

        return LoginResponse.from(token);
    }
}