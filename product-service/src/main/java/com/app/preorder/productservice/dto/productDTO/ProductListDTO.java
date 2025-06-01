package com.app.preorder.productservice.dto.productDTO;

import com.app.preorder.productservice.dto.stockDTO.ProductStockDTO;
import com.app.preorder.common.type.CategoryType;
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
    private CategoryType category;
    private List<ProductStockDTO> productStockDTOS;
}
