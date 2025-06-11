package com.app.preorder.cartservice.service.cart;


import com.app.preorder.cartservice.dto.cart.CartItemResponse;
import com.app.preorder.cartservice.domain.entity.CartItem;
import com.app.preorder.common.dto.ProductInternal;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CartService {

    // 회원가입시 카트 생성
    void createCartForMember(Long memberId);

    // 카트에 아이템 추가
    void addItem(Long memberId, Long productId, Long quantity);

    // 카트 아이템 수량 감소
    void decreaseItem(Long memberId, Long productId, Long quantity);

    // 카트에 아이템 삭제
    void deleteItem(List<String> cartItemIds);

    // 카트 목록 조회
    Page<CartItemResponse> getCartItemListWithPaging(int page, Long memberId);

    // 카트 아이템 하나 전체 조회
    CartItem getAllCartItemInfo(Long cartItemId);

}
