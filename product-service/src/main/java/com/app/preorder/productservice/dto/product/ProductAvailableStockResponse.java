package com.app.preorder.productservice.dto.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ProductAvailableStockResponse {
    private Long productId;
    private Long availableQuantity;
}
