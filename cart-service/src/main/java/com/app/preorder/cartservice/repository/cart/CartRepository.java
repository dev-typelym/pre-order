package com.app.preorder.cartservice.repository.cart;


import com.app.preorder.cartservice.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long>, CartQueryDsl {

}
