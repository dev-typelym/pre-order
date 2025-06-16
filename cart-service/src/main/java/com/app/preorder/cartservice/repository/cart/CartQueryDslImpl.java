package com.app.preorder.cartservice.repository.cart;

import com.app.preorder.cartservice.domain.entity.Cart;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Optional;


@RequiredArgsConstructor
public class CartQueryDslImpl implements CartQueryDsl{

    private final JPAQueryFactory query;

}
