package com.app.preorder.repository.product;

import com.app.preorder.entity.product.Stock;

public interface StockQueryDsl {

    //    상품id로 재고 조회
    public Stock findStockByProductId_queryDSL(Long productId);
}
