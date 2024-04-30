package com.app.preorder.domain.cartDTO;

import com.app.preorder.entity.cart.Cart;
import com.app.preorder.entity.product.Product;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartItemListDTO {

    private Long id;
    private Cart cart;
    private Product product;
    private Long count;

}
