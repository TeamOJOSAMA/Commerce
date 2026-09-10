package com.example.commerce.domain.cart.service;

import com.example.commerce.domain.auth.entity.AuthUser;
import com.example.commerce.domain.cart.dto.AddToCartRequest;
import com.example.commerce.domain.cart.dto.AddToCartResponse;
import com.example.commerce.domain.cart.entity.Cart;
import com.example.commerce.domain.cart.entity.CartItem;
import com.example.commerce.domain.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션으로 설정
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;

//    public AddToCartResponse createCart(AuthUser authUser, Long productId, AddToCartRequest request) {
//        Cart cart = new Cart(authUser);
//        CartItem cartItem = new CartItem(cart, );
//        return AddToCartResponse.from(cartItem);
//    }
}
