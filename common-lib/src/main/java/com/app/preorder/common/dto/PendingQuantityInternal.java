package com.app.preorder.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PendingQuantityInternal {
    private Long productId;
    private Long quantity;
}
