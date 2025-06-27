package com.app.preorder.productservice.repository;

import com.app.preorder.productservice.domain.entity.Stock;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Optional;


@RequiredArgsConstructor
public class StockQueryDslImpl implements StockQueryDsl {

    private final JPAQueryFactory query;

    @Override
    public Optional<Stock> findStockById(Long productId) {
        QStock stock = QStock.stock;

        return Optional.ofNullable(
                query.selectFrom(stock)
                        .where(stock.product.id.eq(productId))
                        .fetchOne()
        );
    }

}
