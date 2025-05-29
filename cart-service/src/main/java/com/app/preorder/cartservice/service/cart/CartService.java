package com.app.preorder.cartservice.service.cart;


import com.app.preorder.cartservice.domain.cartDTO.CartItemListDTO;
import com.app.preorder.cartservice.entity.CartItem;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CartService {

    // 카트에 아이템 추가
    public void addItem(Long memberId, Long productId, Long quantity);

    // 카트 아이템 수량 감소
    public void decreaseItem(Long memberId, Long productId, Long quantity);

    // 카트에 아이템 삭제
    public void deleteItem(List<String> cartItemIds);

    // 카트 목록 조회
    public Page<CartItemListDTO> getCartItemListWithPaging(int page, Long memberId);

    // 카트 아이템 하나 전체 조회
    public CartItem getAllCartItemInfo(Long cartItemId);

    default CartItemListDTO toCartItemListDTO(CartItem cartItem) {
        return CartItemListDTO.builder()
                .id(cartItem.getId())
                .count(cartItem.getCount())
                .cart(cartItem.getCart())
                .product(cartItem.getProduct())
                .build();
    }
}
