package com.app.preorder.productservice.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class StockQueryDslImpl implements StockQueryDsl {

    private final JPAQueryFactory query;

}
