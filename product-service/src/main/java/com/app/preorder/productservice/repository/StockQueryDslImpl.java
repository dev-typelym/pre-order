package com.app.preorder.productservice.repository;

import com.app.preorder.entity.product.Stock;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import static com.app.preorder.entity.product.QProduct.product;
import static com.app.preorder.entity.product.QStock.stock;

@RequiredArgsConstructor
public class StockQueryDslImpl implements StockQueryDsl {

    private final JPAQueryFactory query;

    // 상품아이디로 재고 조회
    public Stock findStockByProductId_queryDSL(Long productId){
        return query.selectFrom(stock)
                .where(stock.product.id.eq(productId))
                .fetchOne();
    }
}
