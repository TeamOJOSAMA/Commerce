package com.example.commerce.domain.user.repository;

import com.example.commerce.domain.user.entity.QUser;
import com.example.commerce.domain.user.entity.User;
import com.example.commerce.domain.user.entity.UserRole;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.security.PrivateKey;
import java.util.List;

@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Page<User> searchUsers(UserRole role, Pageable pageable) {
        QUser user = QUser.user;

        List<User> content = jpaQueryFactory
                .selectFrom(user)
                .where(roleEq(role, user))
                .orderBy(user.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(user.count())
                .from(user)
                .where(roleEq(role, user));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression roleEq(UserRole role, QUser user) {

        return role != null ? user.role.eq(role) : null;
    }
}
