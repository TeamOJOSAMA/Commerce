package com.example.commerce.domain.product.repository;

import com.example.commerce.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {

    Page<Product> findAllByOrderByViewCountDesc(Pageable pageable);
}
