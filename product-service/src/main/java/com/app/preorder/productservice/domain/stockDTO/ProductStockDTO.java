package com.app.preorder.productservice.domain.stockDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductStockDTO {

    private Long id;
    private Long stockQuantity;


    @Builder
    public ProductStockDTO(Long id,Long stockQuantity) {
        this.id = id;
        this.stockQuantity = stockQuantity;
    }
}
