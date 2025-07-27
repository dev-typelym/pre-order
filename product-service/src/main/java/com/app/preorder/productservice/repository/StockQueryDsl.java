package com.app.preorder.productservice.repository;


import com.app.preorder.productservice.domain.entity.Stock;

import java.util.List;

public interface StockQueryDsl {

    // 상품 ID 목록으로 재고 조회 (다건)
    List<Stock> findByProductIds(List<Long> productIds);

}
