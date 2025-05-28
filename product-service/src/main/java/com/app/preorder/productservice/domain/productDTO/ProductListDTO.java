package com.app.preorder.productservice.domain.productDTO;

import com.app.preorder.productservice.domain.stockDTO.ProductStockDTO;
import com.app.preorder.type.CatergoryType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ProductListDTO {

    private Long id;
    private String productName;
    private BigDecimal productPrice;
    private String description;
    private CatergoryType category;
    private List<ProductStockDTO> productStockDTOS;
}
