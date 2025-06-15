package com.app.preorder.cartservice.dto.cart;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AddCartItemRequest {
    private Long productId;
    private Long count;
}
