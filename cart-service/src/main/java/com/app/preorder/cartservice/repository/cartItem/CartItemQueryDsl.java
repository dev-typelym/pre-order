package com.app.preorder.cartservice.repository.cartItem;

import com.app.preorder.cartservice.domain.entity.CartItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


public interface CartItemQueryDsl {

    // 카트아이템 삭제
    void deleteCartItemsByIdsAndMemberId(List<Long> cartItemIds, Long memberId);

    //  상품 목록 조회
    Page<CartItem> findCartItemsByMemberId(Pageable pageable, Long memberId);

}
