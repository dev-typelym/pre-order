package com.app.preorder.repository.cart;

import com.app.preorder.entity.cart.Cart;
import com.app.preorder.repository.member.MemberQueryDsl;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long>, CartQueryDsl {

}
