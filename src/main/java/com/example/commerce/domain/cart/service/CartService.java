package com.example.commerce.domain.cart.service;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.cart.dto.GetCartResponse;
import com.example.commerce.domain.product.entity.Product;
import com.example.commerce.domain.cart.entity.Cart;
import com.example.commerce.domain.cart.entity.CartItem;
import com.example.commerce.domain.cart.repository.CartRepository;
import com.example.commerce.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션으로 설정
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;

    @Transactional
    public CartItem addCartItem(User user, Product product, Integer quantity) {

        // 현재 판매중인 상품인가
        //if (product.getStatus().equals()) TODO: ProductStatus.ACTIVE 가 생겨야 작성 가능

        // 해당 상품의 재고가 충분한가
        if (quantity > product.getStock()) throw new BusinessException(ErrorCode.STOCK_AMOUNT_OVERFLOW); // 400

        // 회원DB 내의 이 회원이 기존 장바구니를 갖고있는가... 없다면 이 회원 명의로 새 장바구니 생성
        Cart cart = cartRepository.findCartByUser(user)
                .orElseGet(() -> new Cart(user));

        // 장바구니 아이템 생성
        CartItem cartItem = new CartItem(cart, product, quantity);

        // 그 장바구니 아이템을 장바구니에 담기
        cart.addCartItem(cartItem);
        cartRepository.save(cart);

        return cartItem;
    }

    public GetCartResponse getCart(User user) {

        // 로그인 회원 ID로 본인의 장바구니를 조회
        return cartRepository.findCartByUser(user)
                .map(GetCartResponse::from) // 장바구니가 있다면 (단, 장바구니 내의 내용물은 있든 없든 상관없음)
                .orElseGet(GetCartResponse::empty); // 장바구니 자체가 없음
    }

}
