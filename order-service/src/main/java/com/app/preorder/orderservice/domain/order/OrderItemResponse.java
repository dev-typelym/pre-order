package com.app.preorder.orderservice.domain.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class OrderItemResponse {
    private Long productId;
    private String productName;
    private BigDecimal orderPrice;     // 단품 가격
    private Long quantity;
    private BigDecimal totalPrice;     // 단품 소계
}
