package com.app.preorder.cartservice.repository.cart;


import com.app.preorder.cartservice.domain.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long>, CartQueryDsl {
    Optional<Cart> findCartByMemberId(Long memberId);
    boolean existsByMemberId(Long memberId);
}
