package com.app.preorder.cartservice.service.cart;


import com.app.preorder.cartservice.dto.cart.CartItemResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CartService {

    // 회원가입시 카트 생성
    void createCartForMember(Long memberId);

    // 카트 존재 보장
    void ensureCartExists(Long memberId);

    // 카트에 아이템 추가
    void addCartItem(Long memberId, Long productId, Long quantity);

    // 카트 아이템 수량 감소
    void decreaseCartItem(Long memberId, Long productId, Long quantity);

    // 카트에 아이템 삭제
    void deleteCartItems(Long memberId, List<Long> cartItemIds);

    // 카트 목록 조회
    Page<CartItemResponse> getCartItemsWithPaging(int page, Long memberId);


}
