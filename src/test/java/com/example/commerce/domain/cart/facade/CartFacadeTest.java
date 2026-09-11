package com.example.commerce.domain.cart.facade;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.cart.dto.AddToCartRequest;
import com.example.commerce.domain.cart.dto.AddToCartResponse;
import com.example.commerce.domain.cart.entity.Cart;
import com.example.commerce.domain.cart.entity.CartItem;
import com.example.commerce.domain.cart.service.CartService;
import com.example.commerce.domain.product.entity.Product;
import com.example.commerce.domain.product.repository.ProductRepository;
import com.example.commerce.domain.user.entity.User;
import com.example.commerce.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartFacadeTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserService userService;

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartFacade cartFacade;

    @Test
    @DisplayName("회원과 상품이 존재하면 장바구니 서비스에 상품 담기를 요청하고 응답을 반환한다")
    void addCartItemSuccessfully() {
        // given
        Long userId = 1L;
        Long productId = 10L;
        int quantity = 2;
        Long cartItemId = 100L;
        LocalDateTime createdAt =
                LocalDateTime.of(2026, 9, 11, 10, 0);

        AddToCartRequest request =
                new AddToCartRequest(quantity);

        User user = mock(User.class);
        Product product = mock(Product.class);
        Cart cart = mock(Cart.class);
        CartItem cartItem = mock(CartItem.class);

        when(userService.findUser(userId)).thenReturn(user);
        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));
        when(cartService.addCartItem(user, product, quantity))
                .thenReturn(cartItem);

        when(cartItem.getId()).thenReturn(cartItemId);
        when(cartItem.getProduct()).thenReturn(product);
        when(product.getId()).thenReturn(productId);
        when(cartItem.getQuantity()).thenReturn(quantity);
        when(cartItem.getCart()).thenReturn(cart);
        when(cart.getCreatedAt()).thenReturn(createdAt);

        // when
        AddToCartResponse response =
                cartFacade.addCartItem(userId, productId, request);

        // then
        assertAll(
                () -> assertEquals(cartItemId, response.cartItemId()),
                () -> assertEquals(productId, response.productId()),
                () -> assertEquals(quantity, response.quantity()),
                () -> assertEquals(createdAt, response.createdAt())
        );

        verify(userService).findUser(userId);
        verify(productRepository).findById(productId);
        verify(cartService).addCartItem(user, product, quantity);
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 회원 없음 예외가 발생한다")
    void throwExceptionWhenUserDoesNotExist() {
        // given
        Long userId = 1L;
        Long productId = 10L;
        AddToCartRequest request = new AddToCartRequest(2);

        when(userService.findUser(userId))
                .thenThrow(
                        new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
                );

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> cartFacade.addCartItem(
                        userId,
                        productId,
                        request
                )
        );

        // then
        assertEquals(
                ErrorCode.MEMBER_NOT_FOUND,
                exception.getErrorCode()
        );

        verifyNoInteractions(productRepository, cartService);
    }

    @Test
    @DisplayName("존재하지 않는 상품이면 상품 없음 예외가 발생한다")
    void throwExceptionWhenProductDoesNotExist() {
        // given
        Long userId = 1L;
        Long productId = 10L;
        AddToCartRequest request = new AddToCartRequest(2);
        User user = mock(User.class);

        when(userService.findUser(userId)).thenReturn(user);
        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> cartFacade.addCartItem(
                        userId,
                        productId,
                        request
                )
        );

        // then
        assertEquals(
                ErrorCode.PRODUCT_NOT_FOUND,
                exception.getErrorCode()
        );

        verify(userService).findUser(userId);
        verify(productRepository).findById(productId);
        verifyNoInteractions(cartService);
    }
}