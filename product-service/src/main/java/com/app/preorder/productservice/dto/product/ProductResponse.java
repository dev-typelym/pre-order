package com.app.preorder.productservice.dto.product;

import com.app.preorder.common.type.ProductStatus;
import com.app.preorder.common.type.CategoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private String productName;
    private BigDecimal productPrice;
    private String description;
    private CategoryType category;
    private ProductStatus status;
    private Long availableQuantity;
}
