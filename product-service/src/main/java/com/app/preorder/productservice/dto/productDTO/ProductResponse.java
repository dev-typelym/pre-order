package com.app.preorder.productservice.dto.productDTO;

import com.app.preorder.productservice.dto.stockDTO.ProductStockResponse;
import com.app.preorder.common.type.CategoryType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class ProductResponse {
    private Long id;
    private String productName;
    private BigDecimal productPrice;
    private String description;
    private CategoryType category;
    private List<ProductStockResponse> stocks;
}
