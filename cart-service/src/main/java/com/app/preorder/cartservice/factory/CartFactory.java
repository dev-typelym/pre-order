package com.app.preorder.cartservice.factory;

import com.app.preorder.cartservice.domain.entity.CartItem;
import com.app.preorder.cartservice.dto.cart.CartItemResponse;
import com.app.preorder.common.dto.ProductInternal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CartFactory {

    public CartItemResponse createCartItemResponse(CartItem cartItem, ProductInternal product) {
        return CartItemResponse.builder()
                .id(cartItem.getId())
                .cartId(cartItem.getCart().getId())
                .count(cartItem.getCount())
                .product(product)
                .build();
    }

}