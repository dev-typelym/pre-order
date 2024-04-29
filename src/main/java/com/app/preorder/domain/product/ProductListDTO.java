package com.app.preorder.domain.product;

import com.app.preorder.domain.stock.ProductStockDTO;
import com.app.preorder.entity.product.Stock;
import com.app.preorder.type.CatergoryType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProductListDTO {

    private Long id;
    private String productName;
    private Long productPrice;
    private String description;
    private CatergoryType category;
    private List<ProductStockDTO> productStockDTOS;
}
