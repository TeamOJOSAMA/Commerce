package com.example.commerce.domain.user.service;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.user.dto.UserResponse;
import com.example.commerce.domain.user.entity.User;
import com.example.commerce.domain.user.entity.UserRole;
import com.example.commerce.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final long USER_ID = 1L;
    private static final String NAME = "tester";
    private static final String EMAIL = "test@test.com";
    private static final String ENCODED_PASSWORD = "encoded-password";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // -------- getMe --------

    @Test
    @DisplayName("내 정보 조회 성공")
    void getMe_success() {
        User user = User.of(NAME, EMAIL, ENCODED_PASSWORD, UserRole.USER);

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        UserResponse response = userService.getMe(USER_ID);

        assertThat(response.name()).isEqualTo(NAME);
        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.role()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("존재하지 않는 유저를 조회하면 MEMBER_NOT_FOUND")
    void getMe_notFound() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMe(USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    // -------- getUsers --------

    @Test
    @DisplayName("유저 목록 조회 - 검색 결과를 UserResponse로 변환한다")
    void getUsers_success() {
        User user = User.of(NAME, EMAIL, ENCODED_PASSWORD, UserRole.SELLER);
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> page = new PageImpl<>(List.of(user), pageable, 1);

        given(userRepository.searchUsers(UserRole.SELLER, pageable)).willReturn(page);

        Page<UserResponse> response = userService.getUsers(UserRole.SELLER, pageable);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).role()).isEqualTo(UserRole.SELLER);
    }

    // -------- updateRole --------

    @Test
    @DisplayName("권한 변경 성공 - 관리자가 여러 명이면 강등 가능하다")
    void updateRole_success() {
        User user = User.of(NAME, EMAIL, ENCODED_PASSWORD, UserRole.ADMIN);

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(userRepository.countByRole(UserRole.ADMIN)).willReturn(2L);

        userService.updateRole(USER_ID, UserRole.USER);

        assertThat(user.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("마지막 관리자를 강등하려 하면 LAST_ADMIN_CANNOT_BE_DEMOTED")
    void updateRole_lastAdmin_blocked() {
        User user = User.of(NAME, EMAIL, ENCODED_PASSWORD, UserRole.ADMIN);

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(userRepository.countByRole(UserRole.ADMIN)).willReturn(1L);

        assertThatThrownBy(() -> userService.updateRole(USER_ID, UserRole.USER))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.LAST_ADMIN_CANNOT_BE_DEMOTED);

        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("ADMIN이 아닌 유저의 권한 변경은 관리자 수 체크를 하지 않는다")
    void updateRole_nonAdminTarget_noCountCheck() {
        User user = User.of(NAME, EMAIL, ENCODED_PASSWORD, UserRole.USER);

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        userService.updateRole(USER_ID, UserRole.SELLER);

        assertThat(user.getRole()).isEqualTo(UserRole.SELLER);
    }

    @Test
    @DisplayName("존재하지 않는 유저의 권한을 변경하면 MEMBER_NOT_FOUND")
    void updateRole_notFound() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateRole(USER_ID, UserRole.ADMIN))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }
}
