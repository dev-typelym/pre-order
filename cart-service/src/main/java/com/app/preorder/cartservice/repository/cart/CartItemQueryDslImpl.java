package com.app.preorder.cartservice.repository.cart;


import com.app.preorder.cartservice.entity.CartItem;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;


@RequiredArgsConstructor
public class CartItemQueryDslImpl implements CartItemQueryDsl {
    private final JPAQueryFactory query;

    // 카트 아이템 수량 감소
    @Override
    public void deleteCartItemByIds_queryDSL(Long cartItemId) {
        query.delete(cartItem)
                .where(cartItem.id.in(cartItemId))
                .execute();
    }

    // 카트 아이템 목록
    @Override
    public Page<CartItem> findAllCartItem_queryDSL(Pageable pageable, Long memberId) {

        QCartItem cartItem = QCartItem.cartItem;
        List<CartItem> foundCartItem = query.select(cartItem)
                .from(cartItem)
                .where(cartItem.cart.member.id.eq(memberId))
                .orderBy(cartItem.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long count = query.select(cartItem.count())
                .from(cartItem)
                .where(cartItem.cart.member.id.eq(memberId))
                .fetchOne();

        return new PageImpl<>(foundCartItem, pageable, count);
    }

    // 카트아이템 하나 정보 조회
    public CartItem findCartItemById_queryDSL(Long cartItemId){
        return query.selectFrom(cartItem)
                .where(cartItem.id.eq(cartItemId))
                .fetchOne();
    }
}
