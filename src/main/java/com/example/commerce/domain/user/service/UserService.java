package com.example.commerce.domain.user.service;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
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
@Transactional(readOnly = true)
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

        // 본인말고 ADMIN이 없는 상태에서 자신을 바꾸려고 한다면 예외처리
        if (user.getRole() == UserRole.ADMIN
        && role != UserRole.ADMIN
        && userRepository.countByRole(UserRole.ADMIN) <= 1) {
            throw new BusinessException(ErrorCode.LAST_ADMIN_CANNOT_BE_DEMOTED);
        }

        user.changeRole(role);
    }

    private User findUser(Long userId) {

        return userRepository.findById(userId).orElseThrow(
                () -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}