package com.app.preorder.cartservice.repository.cart;


import com.app.preorder.cartservice.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long>, CartItemQueryDsl{
}
