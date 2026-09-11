package com.example.commerce.domain.event.repository;

import com.example.commerce.domain.event.dto.EventResponse;
import com.example.commerce.domain.event.entity.EventStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static com.example.commerce.domain.event.entity.QEvent.event;

@RequiredArgsConstructor
public class EventCustomImpl implements EventCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<EventResponse> searchEvents(EventStatus status, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        if(status != null){
            builder.and(event.status.eq(status));
        }
        List<EventResponse> result =  queryFactory
                .select(Projections.constructor(EventResponse.class,
                        event.id,
                        event.product.id,
                        event.discountRate,
                        event.totalQuantity,
                        event.soldQuantity,
                        event.startAt,
                        event.endAt,
                        event.status
                ))
                .from(event)
                .where(builder)
                .orderBy(event.startAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(event.count())
                .from(event)
                .where(builder);

        return PageableExecutionUtils.getPage(result,pageable, () -> {
            Long count = countQuery.fetchOne();
            return count != null ? count : 0L;
        });

    }

}