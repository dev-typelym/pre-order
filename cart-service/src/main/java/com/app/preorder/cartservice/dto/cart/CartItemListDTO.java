package com.app.preorder.cartservice.dto.cart;

import com.app.preorder.common.dto.ProductResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartItemListDTO {

    private Long id;
    private Long cartId;                    // Cart 엔티티 ID만 포함
    private ProductResponse product;        // FeignClient로 받아온 DTO
    private Long count;

}
