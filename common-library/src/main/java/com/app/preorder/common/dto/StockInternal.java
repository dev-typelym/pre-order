package com.app.preorder.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockInternal {
    private Long productId;      // 상품 ID
    private Long stockQuantity;  // 재고 수량
}
