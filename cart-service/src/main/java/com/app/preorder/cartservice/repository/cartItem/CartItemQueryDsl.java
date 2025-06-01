package com.app.preorder.cartservice.repository.cartItem;

import com.app.preorder.cartservice.domain.entity.CartItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface CartItemQueryDsl {

    // 카트아이템 삭제
    public void deleteCartItemByIds_queryDSL(Long cartItemId);

    //  상품 목록 조회
    public Page<CartItem> findAllCartItem_queryDSL(Pageable pageable, Long memberId);

    // 카트아이템 하나 정보 조회
    public CartItem findCartItemById_queryDSL(Long cartItemId);
}
