package com.app.preorder.cartservice.repository.cart;


import com.app.preorder.cartservice.entity.Cart;

public interface CartQueryDsl {

    // 멤버 아이디로 카트 찾기
    public Cart findCartByMemberId(Long memberId);
}
