package com.app.preorder.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class ProductResponse {
    private Long productId;
    private String productName;
    private BigDecimal productPrice;
    private String description;
    private String category;
}