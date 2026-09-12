package com.example.commerce.domain.cart.service;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.cart.dto.GetCartResponse;
import com.example.commerce.domain.cart.dto.UpdateQuantityRequest;
import com.example.commerce.domain.cart.dto.UpdateQuantityResponse;
import com.example.commerce.domain.cart.repository.CartItemRepository;
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
    private final CartItemRepository cartItemRepository;

    /** (CartFacade에서 회원과 상품을 조회한 후 호출) */
    @Transactional
    public CartItem addCartItem(User user, Product product, Integer quantity) {

        // 현재 판매중인 상품인가
        //if (product.getStatus().equals()) TODO: ProductStatus.ACTIVE 가 생겨야 작성 가능

        // 해당 상품의 재고가 충분한가
        if (quantity > product.getStock())
            throw new BusinessException(ErrorCode.STOCK_AMOUNT_OVERFLOW); // 400

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

    /** 장바구니 조회 */
    public GetCartResponse getCart(Long userId) {

        // 로그인 회원 ID로 본인의 장바구니를 조회
        return cartRepository.findByUserId(userId)
                .map(GetCartResponse::from) // 장바구니가 있다면 (단, 장바구니 내의 내용물은 있든 없든 상관없음)
                .orElseGet(GetCartResponse::empty); // 장바구니 자체가 없다면
    }

    /** 요청한 회원 소유의 장바구니의 특정 상품 수량 변경 */
    @Transactional
    public UpdateQuantityResponse updateQuantity(Long userId, Long cartItemId, UpdateQuantityRequest request) {

        CartItem cartItem = findCartItem(userId, cartItemId);

        // 요청 수량이 판매중 상품의 총 수량을 초과하지 않는가
        if (request.quantity() > cartItem.getProduct().getStock())
            throw new BusinessException(ErrorCode.STOCK_AMOUNT_OVERFLOW); // 400

        // 해당 상품 수량 변경
        cartItem.updateQuantity(request.quantity());

        // Dirty Checking...

        // updatedAt이 응답 생성 전에 갱신되도록 반영
        cartItemRepository.flush();

        return UpdateQuantityResponse.from(cartItem);
    }

    /** 요청한 회원 소유의 장바구니의 특정 상품 삭제 */
    @Transactional
    public void deleteCartItem(Long userId, Long cartItemId) {

        CartItem cartItem = findCartItem(userId, cartItemId);

        // 해당 상품 삭제
        cartItemRepository.delete(cartItem);
    }

    /** 요청한 회원 소유의 장바구니 비우기 */
    @Transactional
    public void deleteAllCartItem(Long userId) {

        // 이 회원이 장바구니를 갖고있는지 확인
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_EMPTY));

        // 장바구니 비우기
        cart.getCartItems().clear();
    }

    private CartItem findCartItem(Long userId, Long cartItemId) {

        // 해당 장바구니에 상품이 있고, 그 상품의 소유자가 있는가
        return cartItemRepository.findByIdAndCartUserId(cartItemId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND)); // 없다면 404
    }
}
