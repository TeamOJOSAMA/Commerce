package com.example.commerce.domain.cart.repository;

import com.example.commerce.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // 사용자 장바구니 전체를 조회한다. 연관 상품은 잠금 전에 로딩하지 않도록 fetch join하지 않는다.
    List<CartItem> findAllByCartUserId(Long userId);

    // 선택 ID 중 본인 소유 항목만 반환한다. 누락된 요청 항목이 있는지는 Facade가 개수로 확인한다.
    List<CartItem> findAllByCartUserIdAndIdIn(Long userId, List<Long> cartItemIds);

    // 장바구니내 상품 ID와 회원 ID 같이 조회
    Optional<CartItem> findByIdAndCartUserId(Long cartItemId, Long userId);
}
