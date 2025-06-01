package com.app.preorder.cartservice.repository.cartItem;


import com.app.preorder.cartservice.domain.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long>, CartItemQueryDsl{
}
