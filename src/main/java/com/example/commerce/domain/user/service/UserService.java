package com.example.commerce.domain.user.service;

import com.example.commerce.common.error.BusinessException;
import com.example.commerce.common.error.ErrorCode;
import com.example.commerce.domain.user.dto.UserResponse;
import com.example.commerce.domain.user.entity.User;
import com.example.commerce.domain.user.entity.UserRole;
import com.example.commerce.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getMe(Long userId) {
        User user = findUser(userId);

        return UserResponse.from(user);
    }

    public Page<UserResponse> getUsers(UserRole role, Pageable pageable) {

        return userRepository.searchUsers(role, pageable)
                .map(UserResponse::from);
    }

    @Transactional
    public void updateRole(Long userId, UserRole role) {
        User user = findUser(userId);
        user.changeRole(role);
    }

    private User findUser(Long userId) {

        return userRepository.findById(userId).orElseThrow(
                () -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}