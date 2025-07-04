package com.app.preorder.productservice.dto.product;

import com.app.preorder.common.type.CategoryType;
import com.app.preorder.common.type.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductUpdateRequest {
    private String productName;
    private BigDecimal productPrice;
    private String description;
    private CategoryType category;
    private ProductStatus status;
    private LocalDateTime saleStartAt;
    private LocalDateTime saleEndAt;
}

