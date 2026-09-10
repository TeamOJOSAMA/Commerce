package com.example.commerce.domain.product.repository;

import com.example.commerce.domain.product.dto.ProductResponse;
import com.example.commerce.domain.product.dto.SearchProductRequest;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.example.commerce.domain.product.entity.QProduct.product;

@RequiredArgsConstructor
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ProductResponse> searchProductByConditionPage(SearchProductRequest request, Pageable pageable) {
        BooleanBuilder builder = searchCondition(request);

        List<ProductResponse> content = queryFactory
                .select(Projections.constructor(ProductResponse.class,
                        product.id,
                        product.name,
                        product.description,
                        product.category,
                        product.price,
                        product.stock,
                        product.status,
                        product.eventPrice,
                        product.discountRate
                ))
                .from(product)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(product.count())
                .from(product)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private BooleanBuilder searchCondition(SearchProductRequest request) {
        BooleanBuilder builder = new BooleanBuilder();

        if (request.name() != null && !request.name().isBlank()) {
            builder.and(product.name.contains(request.name()));
        }
        if (request.category() != null) {
            builder.and(product.category.eq(request.category()));
        }
        if (request.minPrice() != null) {
            builder.and(product.price.goe(request.minPrice()));
        }
        if (request.maxPrice() != null) {
            builder.and(product.price.loe(request.maxPrice()));
        }
        if (request.status() != null) {
            builder.and(product.status.eq(request.status()));
        }

        return builder;
    }
}