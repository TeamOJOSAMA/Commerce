package com.example.commerce.domain.cart.repository;

import com.example.commerce.domain.cart.entity.Cart;
import com.example.commerce.domain.cart.entity.CartItem;
import com.example.commerce.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // 같은 상품이 이미 담겨있는지 확인해 중복 행 대신 수량을 합치는 데 사용한다.
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);

    // 주문 생성용 조회다. 연관 상품은 잠금 전에 로딩하지 않도록 fetch join하지 않는다.
    // 호출자는 프록시의 ID만 읽어 잠금 대상 상품 ID를 모은다.
    List<CartItem> findAllByCartUserId(Long userId);

    // 선택 ID 중 본인 소유 항목만 반환한다. 누락된 요청 항목이 있는지는 Facade가 개수로 확인한다.
    List<CartItem> findAllByCartUserIdAndIdIn(Long userId, List<Long> cartItemIds);

    // 미리보기 전용 조회다. 미리보기는 아무것도 잠그지 않으므로 상품을 함께 읽어도 잠금 순서에 영향이 없다.
    // fetch join이 없으면 상품 정보를 읽는 순간 장바구니 항목 수만큼 조회가 추가된다(N+1).
    @Query("""
            select cartItem
            from CartItem cartItem
            join fetch cartItem.product
            where cartItem.cart.user.id = :userId
            """)
    List<CartItem> findAllByCartUserIdWithProduct(@Param("userId") Long userId);

    @Query("""
            select cartItem
            from CartItem cartItem
            join fetch cartItem.product
            where cartItem.cart.user.id = :userId
              and cartItem.id in :cartItemIds
            """)
    List<CartItem> findAllByCartUserIdAndIdInWithProduct(
            @Param("userId") Long userId, @Param("cartItemIds") List<Long> cartItemIds
    );

    // 장바구니내 상품 ID와 회원 ID 같이 조회
    Optional<CartItem> findByIdAndCartUserId(Long cartItemId, Long userId);
}
