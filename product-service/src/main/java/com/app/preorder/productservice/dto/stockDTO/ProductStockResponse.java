package com.app.preorder.productservice.dto.stockDTO;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductStockResponse {
    private Long id;
    private Long stockQuantity;
}
