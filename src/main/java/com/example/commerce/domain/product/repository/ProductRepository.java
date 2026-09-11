package com.example.commerce.domain.product.repository;

import com.example.commerce.domain.product.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {

    Page<Product> findAllByOrderByViewCountDesc(Pageable pageable);

    // 재고 확인과 차감 사이에 다른 주문이 끼어들지 못하게 상품을 잠근다.
    // 여러 상품의 조회 순서를 ID 오름차순으로 맞춰 잠금 순서가 뒤집히는 경우를 줄인다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select product
            from Product product
            where product.id in :productIds
            order by product.id asc
            """)
    List<Product> findAllByIdsForUpdate(@Param("productIds") List<Long> productIds);
}
