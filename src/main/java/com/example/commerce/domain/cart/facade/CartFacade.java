package com.example.commerce.domain.cart.facade;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.cart.dto.AddToCartRequest;
import com.example.commerce.domain.cart.dto.AddToCartResponse;
import com.example.commerce.domain.cart.entity.CartItem;
import com.example.commerce.domain.cart.service.CartService;
import com.example.commerce.domain.product.entity.Product;
import com.example.commerce.domain.product.repository.ProductRepository;
import com.example.commerce.domain.user.entity.User;
import com.example.commerce.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class CartFacade { // Facade가 Service를 둘러싸고 있다고 이해하면된다

    private final ProductRepository productRepository;
    private final UserService userService;
    private final CartService cartService;

    public AddToCartResponse addCartItem(Long userId, Long productId, AddToCartRequest request) {

        // 요청 userId에 해당하는 회원이 UserRepository에 있는가
        User user = userService.findUser(userId); // 없다면 404

        // 요청 productId에 해당하는 상품이 productRepository에 있는가
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)); // 404

        // 비즈니스 로직 진행
        CartItem cartItem = cartService.addCartItem(user, product, request.quantity());

        return AddToCartResponse.from(cartItem);
    }
}
