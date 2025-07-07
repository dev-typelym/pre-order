package com.app.preorder.cartservice.repository.cart;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;



@RequiredArgsConstructor
public class CartQueryDslImpl implements CartQueryDsl{

    private final JPAQueryFactory query;

}
