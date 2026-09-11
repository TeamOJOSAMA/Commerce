package com.example.commerce.domain.cart.facade;

import com.example.commerce.domain.cart.dto.AddToCartRequest;
import com.example.commerce.domain.cart.dto.AddToCartResponse;
import com.example.commerce.domain.cart.dto.GetCartResponse;
import com.example.commerce.domain.cart.entity.CartItem;
import com.example.commerce.domain.cart.service.CartService;
import com.example.commerce.domain.product.entity.Product;
import com.example.commerce.domain.product.service.ProductService;
import com.example.commerce.domain.user.entity.User;
import com.example.commerce.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Facade가 Service를 둘러싸고 있다고 이해하면된다. Facade를 거치지 않는 트랜잭션이 있을 수 있다. */
@Component
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션으로 설정
@RequiredArgsConstructor
public class CartFacade {

    private final ProductService productService;
    private final UserService userService;
    private final CartService cartService;

    // 상품 담기
    @Transactional
    public AddToCartResponse addCartItem(Long userId, Long productId, AddToCartRequest request) {

        // 요청 userId에 해당하는 회원이 UserRepository에 있는가
        User user = userService.findUser(userId); // 없다면 404

        // 요청 productId에 해당하는 상품이 productRepository에 있는가
        Product product = productService.findProduct(productId); // 없다면 404

        // 비즈니스 로직 진행
        CartItem cartItem = cartService.addCartItem(user, product, request.quantity());

        return AddToCartResponse.from(cartItem);
    }

    // 장바구니 조회
    public GetCartResponse getCart(Long userId) {

        // 요청 userId에 해당하는 회원이 UserRepository에 있는가
        User user = userService.findUser(userId); // 없다면 404

        // 비즈니스 로직 진행
        return cartService.getCart(user);
    }
}
