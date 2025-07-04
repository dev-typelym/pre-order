package com.app.preorder.productservice.dto.product;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchRequest {
    private String productName;
}