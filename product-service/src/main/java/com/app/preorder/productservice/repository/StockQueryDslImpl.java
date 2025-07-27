package com.app.preorder.productservice.repository;

import com.app.preorder.productservice.domain.entity.QStock;
import com.app.preorder.productservice.domain.entity.Stock;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;


@RequiredArgsConstructor
public class StockQueryDslImpl implements StockQueryDsl {

    private final JPAQueryFactory query;

    // 상품 ID 목록으로 재고 조회 (다건)
    @Override
    public List<Stock> findByProductIds(List<Long> productIds) {
        QStock stock = QStock.stock;

        return query.selectFrom(stock)
                .where(stock.product.id.in(productIds))
                .fetch();
    }

}

