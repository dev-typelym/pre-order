package com.app.preorder.repository.cart;

import com.app.preorder.entity.cart.Cart;

public interface CartQueryDsl {

    // 멤버 아이디로 카트 찾기
    public Cart findCartByMemberId(Long memberId);
}
