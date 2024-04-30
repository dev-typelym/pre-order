package com.app.preorder.repository.cart;

import com.app.preorder.entity.cart.Cart;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import static com.app.preorder.entity.cart.QCart.cart;

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
