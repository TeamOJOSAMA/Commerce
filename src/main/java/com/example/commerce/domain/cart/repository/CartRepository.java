package com.example.commerce.domain.cart.repository;

import com.example.commerce.domain.cart.entity.Cart;
import com.example.commerce.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findCartByUser(User user);
}
