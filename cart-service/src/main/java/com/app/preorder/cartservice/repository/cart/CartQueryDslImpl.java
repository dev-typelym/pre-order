package com.app.preorder.cartservice.repository.cart;

import com.app.preorder.cartservice.domain.entity.Cart;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class CartQueryDslImpl implements CartQueryDsl{

    private final JPAQueryFactory query;

    // 멤버 id로 카트 찾기
    @Override
    public Cart findCartByMemberId(Long memberId) {
        return query.selectFrom(cart)
                .where(cart.member.id.eq(memberId))
                .fetchOne();
    }
}
