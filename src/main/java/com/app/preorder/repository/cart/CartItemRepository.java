package com.app.preorder.repository.cart;

import com.app.preorder.entity.cart.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long>, CartItemQueryDsl{
}
