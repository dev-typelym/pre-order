package com.app.preorder.cartservice.repository.cartItem;


import com.app.preorder.cartservice.domain.entity.CartItem;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;


@RequiredArgsConstructor
public class CartItemQueryDslImpl implements CartItemQueryDsl {
    private final JPAQueryFactory query;

    // 카트 아이템 삭제
    @Override
    public void deleteCartItemsByIdsAndMemberId(List<Long> cartItemIds, Long memberId) {
        query.delete(cartItem)
                .where(cartItem.id.in(cartItemIds)
                        .and(cartItem.cart.member.id.eq(memberId)))
                .execute();
    }

    // 카트 아이템 목록
    @Override
    public Page<CartItem> findAllByMemberId(Pageable pageable, Long memberId) {
        QCartItem cartItem = QCartItem.cartItem;

        List<CartItem> content = query.selectFrom(cartItem)
                .where(cartItem.cart.member.id.eq(memberId))
                .orderBy(cartItem.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long count = query.select(cartItem.count())
                .from(cartItem)
                .where(cartItem.cart.member.id.eq(memberId))
                .fetchOne();

        return new PageImpl<>(content, pageable, count);
    }

    // 카트 아이템 상세 조회
    public CartItem findCartItemById_queryDSL(Long cartItemId){
        return query.selectFrom(cartItem)
                .where(cartItem.id.eq(cartItemId))
                .fetchOne();
    }
}
