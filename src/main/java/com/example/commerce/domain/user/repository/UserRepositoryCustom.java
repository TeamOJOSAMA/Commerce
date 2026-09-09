package com.example.commerce.domain.user.repository;

import com.example.commerce.domain.user.entity.User;
import com.example.commerce.domain.user.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepositoryCustom {
    Page<User> searchUsers(UserRole role, Pageable pageable);
}
