package com.app.preorder.productservice.repository;


import com.app.preorder.productservice.domain.entity.Stock;

import java.util.Optional;

public interface StockQueryDsl {
    Optional<Stock> findStockById(Long productId);
}
