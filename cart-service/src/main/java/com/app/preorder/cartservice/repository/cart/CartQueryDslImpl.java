package com.app.preorder.cartservice.repository.cart;

import com.app.preorder.cartservice.domain.entity.Cart;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Optional;


@RequiredArgsConstructor
public class CartQueryDslImpl implements CartQueryDsl{

    private final JPAQueryFactory query;

    // 멤버 id로 카트 찾기
    @Override
    public Optional<Cart> findCartByMemberId(Long memberId) {
        return Optional.ofNullable(
                query.selectFrom(cart)
                        .where(cart.member.id.eq(memberId))
                        .fetchOne()
        );
    }
}
