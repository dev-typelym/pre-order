package com.app.preorder.productservice.service;

import com.app.preorder.common.dto.StockInternal;

public interface StockService {
    StockInternal getStockById(Long productId);
}
