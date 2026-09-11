package com.example.commerce.domain.cart.service;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.cart.entity.Cart;
import com.example.commerce.domain.cart.entity.CartItem;
import com.example.commerce.domain.cart.repository.CartRepository;
import com.example.commerce.domain.product.entity.Product;
import com.example.commerce.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private User user;

    @Mock
    private Product product;

    @InjectMocks
    private CartService cartService;

    /** 정상 케이스 */
    @Test
    @DisplayName("기존 장바구니가 있으면 해당 장바구니에 상품을 담고 저장한다")
    void addCartItemToExistingCart() {
        // given
        int quantity = 2;
        Cart existingCart = new Cart(user);

        when(product.getStock()).thenReturn(10);
        when(cartRepository.findCartByUser(user))
                .thenReturn(Optional.of(existingCart));

        // when
        CartItem cartItem =
                cartService.addCartItem(user, product, quantity);

        // then
        assertAll(
                () -> assertSame(existingCart, cartItem.getCart()),
                () -> assertSame(product, cartItem.getProduct()),
                () -> assertEquals(quantity, cartItem.getQuantity()),
                () -> assertTrue(existingCart.getCartItems().contains(cartItem))
        );

        verify(cartRepository).findCartByUser(user);
        verify(cartRepository).save(existingCart);
    }

    @Test
    @DisplayName("장바구니가 없으면 새 장바구니를 생성하고 상품을 담아 저장한다")
    void addCartItemToNewCart() {
        // given
        int quantity = 2;

        when(product.getStock()).thenReturn(10);
        when(cartRepository.findCartByUser(user))
                .thenReturn(Optional.empty());

        ArgumentCaptor<Cart> cartCaptor =
                ArgumentCaptor.forClass(Cart.class);

        // when
        CartItem cartItem =
                cartService.addCartItem(user, product, quantity);

        // then
        verify(cartRepository).save(cartCaptor.capture());

        Cart savedCart = cartCaptor.getValue();

        assertAll(
                () -> assertSame(user, savedCart.getUser()),
                () -> assertSame(savedCart, cartItem.getCart()),
                () -> assertSame(product, cartItem.getProduct()),
                () -> assertEquals(quantity, cartItem.getQuantity()),
                () -> assertTrue(savedCart.getCartItems().contains(cartItem))
        );
    }

    /** 경계값 케이스 */
    @Test
    @DisplayName("요청 수량이 재고와 같으면 상품을 담을 수 있다")
    void addCartItemWhenQuantityEqualsStock() {
        // given
        int stock = 5;
        Cart cart = new Cart(user);

        when(product.getStock()).thenReturn(stock);
        when(cartRepository.findCartByUser(user))
                .thenReturn(Optional.of(cart));

        // when
        CartItem cartItem = assertDoesNotThrow(
                () -> cartService.addCartItem(user, product, stock)
        );

        // then
        assertEquals(stock, cartItem.getQuantity());
        verify(cartRepository).save(cart);
    }

    @Test
    @DisplayName("요청 수량이 재고보다 많으면 재고 초과 예외가 발생한다")
    void throwExceptionWhenQuantityExceedsStock() {
        // given
        when(product.getStock()).thenReturn(5);

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> cartService.addCartItem(user, product, 6)
        );

        // then
        assertEquals(
                ErrorCode.STOCK_AMOUNT_OVERFLOW,
                exception.getErrorCode()
        );

        verifyNoInteractions(cartRepository);
    }
}