package com.app.preorder.productservice.dto.product;

import com.app.preorder.common.type.CategoryType;
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
public class ProductCreateRequest {
    private String productName;
    private BigDecimal productPrice;
    private String description;
    private CategoryType category;
    private Long stockQuantity;
    private LocalDateTime saleStartAt;
    private LocalDateTime saleEndAt;
}
