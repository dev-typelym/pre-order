package com.app.preorder.cartservice.domain.product;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductResponse {
    private Long id;
    private String productName;
    private BigDecimal productPrice;
    private String description;
    private String category;
}