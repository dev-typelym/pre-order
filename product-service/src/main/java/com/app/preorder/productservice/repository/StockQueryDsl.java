package com.app.preorder.productservice.repository;


import com.app.preorder.productservice.domain.entity.Stock;

public interface StockQueryDsl {

    //    상품id로 재고 조회
    public Stock findStockByProductId_queryDSL(Long productId);
}
