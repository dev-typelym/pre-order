package com.app.preorder.repository.cart;

import com.app.preorder.domain.cartDTO.CartItemListDTO;
import com.app.preorder.domain.productDTO.ProductListSearch;
import com.app.preorder.entity.cart.CartItem;
import com.app.preorder.entity.product.Product;
import com.app.preorder.type.CatergoryType;
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
